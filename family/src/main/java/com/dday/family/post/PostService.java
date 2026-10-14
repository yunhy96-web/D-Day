package com.dday.family.post;

import com.dday.family.auth.Me;
import com.dday.family.auth.Perm;
import com.dday.family.common.BadRequest;
import com.dday.family.common.Forbidden;
import com.dday.family.common.NotFound;
import com.dday.family.common.Rs;
import com.dday.family.common.Times;
import com.dday.family.common.Valid;
import com.dday.family.media.MediaService;
import com.dday.family.media.MediaService.Media;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 글(순간·일기)·댓글·하트 (설계 §5). 모든 쿼리는 세션의 family_id 로 거르고,
 * 나만 보기(private) 글은 작성자가 아니면 없는 것처럼(404) 다룬다.
 */
@Service
@RequiredArgsConstructor
public class PostService {
    public static final int PAGE = 20, MAX_MEDIA = 20;
    private static final Set<String> KINDS = Set.of("moment", "diary");
    private static final Set<String> VISIBILITIES = Set.of("family", "private");
    /** 이 글을 볼 수 있는가: 같은 가족 + 안 지워짐 + (가족 공개 또는 내 글) */
    private static final String VISIBLE = "p.family_id=? AND p.deleted_at IS NULL AND (p.visibility='family' OR p.member_id=?)";

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final MediaService media;

    public record Card(long id, String kind, String title, String body, String visibility, String postDate, String createdAt,
                       long authorId, String authorName, String authorColor, List<Media> media,
                       int commentCount, int likeCount, boolean liked) {
        public boolean isPrivate() { return "private".equals(visibility); }
        public boolean diary() { return "diary".equals(kind); }
    }
    public record Comment(long id, long memberId, String name, String color, String body, String createdAt) {}
    public record Detail(Card post, List<Comment> comments, List<String> likers) {}
    public record Write(String kind, String title, String body, String visibility, String postDate, List<Long> mediaIds) {}

    private static final String CARD_SELECT = """
            SELECT p.id, p.kind, p.title, p.body, p.visibility, p.post_date, p.created_at, p.member_id, m.name, m.color,
                   (SELECT COUNT(*) FROM comment c WHERE c.post_id=p.id AND c.deleted_at IS NULL),
                   (SELECT COUNT(*) FROM reaction r WHERE r.post_id=p.id),
                   EXISTS(SELECT 1 FROM reaction r WHERE r.post_id=p.id AND r.member_id=?)
            FROM post p JOIN member m ON m.id = p.member_id
            """;

    /** kind: null=전체 | moment | diary. mine=true 면 내 글만(내 일기 화면) */
    public List<Card> feed(Me me, String kind, boolean mine, int page) {
        List<Object> args = new ArrayList<>(List.of(me.memberId(), me.familyId(), me.memberId()));
        String where = VISIBLE;
        if (kind != null && KINDS.contains(kind)) { where += " AND p.kind=?"; args.add(kind); }
        if (mine) { where += " AND p.member_id=?"; args.add(me.memberId()); }
        args.add(PAGE + 1);
        args.add(Math.max(0, page) * PAGE);
        return cards(me, CARD_SELECT + " WHERE " + where + " ORDER BY p.post_date DESC, p.id DESC LIMIT ? OFFSET ?", args.toArray());
    }

    public Detail detail(Me me, long id) {
        Card c = card(me, id);
        List<Comment> comments = jdbc.query("""
                SELECT c.id, c.member_id, m.name, m.color, c.body, c.created_at FROM comment c JOIN member m ON m.id=c.member_id
                WHERE c.post_id=? AND c.deleted_at IS NULL ORDER BY c.id""",
                (rs, i) -> new Comment(rs.getLong(1), rs.getLong(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6)), id);
        List<String> likers = jdbc.queryForList("SELECT m.name FROM reaction r JOIN member m ON m.id=r.member_id WHERE r.post_id=? ORDER BY r.created_at", String.class, id);
        return new Detail(c, comments, likers);
    }

    public Card card(Me me, long id) {
        List<Card> r = cards(me, CARD_SELECT + " WHERE p.id=? AND " + VISIBLE, me.memberId(), id, me.familyId(), me.memberId());
        if (r.isEmpty()) throw new NotFound();
        return r.get(0);
    }

    private List<Card> cards(Me me, String sql, Object... args) {
        List<Object[]> rows = jdbc.query(sql, (rs, i) -> new Object[]{rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4),
                rs.getString(5), rs.getString(6), rs.getString(7), rs.getLong(8), rs.getString(9), rs.getString(10),
                rs.getInt(11), rs.getInt(12), rs.getBoolean(13)}, args);
        Map<Long, List<Media>> byPost = new LinkedHashMap<>();
        for (Media m : media.ofPosts(me.familyId(), rows.stream().map(r -> (Long) r[0]).toList()))
            byPost.computeIfAbsent(m.postId(), k -> new ArrayList<>()).add(m);
        return rows.stream().map(r -> new Card((long) r[0], (String) r[1], (String) r[2], (String) r[3], (String) r[4], (String) r[5],
                (String) r[6], (long) r[7], (String) r[8], (String) r[9], byPost.getOrDefault((Long) r[0], List.of()),
                (int) r[10], (int) r[11], (boolean) r[12])).toList();
    }

    public long create(Me me, Write w) {
        me.require(Perm.POST_WRITE);
        Write v = validate(w);
        return tx.execute(s -> {
            Long id = jdbc.queryForObject("""
                    INSERT INTO post(family_id, member_id, kind, title, body, visibility, post_date, created_at)
                    VALUES (?,?,?,?,?,?,?,?) RETURNING id""", Long.class,
                    me.familyId(), me.memberId(), v.kind(), v.title(), v.body(), v.visibility(), v.postDate(), Times.now());
            attach(me, id, v.mediaIds());
            return id;
        });
    }

    public void update(Me me, long id, Write w) {
        Card c = card(me, id);
        if (c.authorId() != me.memberId()) throw new Forbidden();
        Write v = validate(w);
        tx.executeWithoutResult(s -> {
            jdbc.update("UPDATE post SET kind=?, title=?, body=?, visibility=?, post_date=?, updated_at=? WHERE id=?",
                    v.kind(), v.title(), v.body(), v.visibility(), v.postDate(), Times.now(), id);
            // 빠진 사진은 떼어 둔다(하루 뒤 정리 작업이 지움)
            jdbc.update("UPDATE media SET post_id=NULL, created_at=? WHERE post_id=? AND family_id=?", Times.now(), id, me.familyId());
            attach(me, id, v.mediaIds());
        });
    }

    /** 내 사진 중 아직 글에 안 붙었거나 이 글에 붙어 있던 것만 */
    private void attach(Me me, long postId, List<Long> ids) {
        for (int i = 0; i < ids.size(); i++) {
            int n = jdbc.update("UPDATE media SET post_id=?, sort=? WHERE id=? AND family_id=? AND member_id=? AND post_id IS NULL",
                    postId, i, ids.get(i), me.familyId(), me.memberId());
            if (n == 0) throw new BadRequest("media");
        }
    }

    private static Write validate(Write w) {
        String kind = Valid.oneOf(w.kind(), KINDS, "kind");
        boolean diary = kind.equals("diary");
        String title = diary ? Valid.optText(w.title(), 100, "title") : null;
        String body = w.body() == null ? "" : w.body().replace("\r\n", "\n");
        body = Valid.optText(body, 20_000, "body");
        String visibility = diary ? Valid.oneOf(w.visibility() == null ? "family" : w.visibility(), VISIBILITIES, "visibility") : "family";
        List<Long> ids = w.mediaIds() == null ? List.of() : w.mediaIds();
        if (ids.size() > MAX_MEDIA || new HashSet<>(ids).size() != ids.size()) throw new BadRequest("media");
        if (body == null && ids.isEmpty()) throw new BadRequest("empty");
        return new Write(kind, title, body == null ? "" : body, visibility, Valid.date(w.postDate()), ids);
    }

    public void delete(Me me, long id) {
        Card c = card(me, id);
        if (c.authorId() != me.memberId() && !me.has(Perm.POST_DELETE_ANY)) throw new Forbidden();
        jdbc.update("UPDATE post SET deleted_at=?, deleted_by=? WHERE id=?", Times.now(), me.memberId(), id);
    }

    public long comment(Me me, long postId, String body) {
        me.require(Perm.COMMENT_WRITE);
        card(me, postId);
        return jdbc.queryForObject("INSERT INTO comment(post_id, member_id, body, created_at) VALUES (?,?,?,?) RETURNING id", Long.class,
                postId, me.memberId(), Valid.text(body, 1000, "comment"), Times.now());
    }

    public void deleteComment(Me me, long commentId) {
        List<long[]> r = jdbc.query("SELECT post_id, member_id FROM comment WHERE id=? AND deleted_at IS NULL",
                (rs, i) -> new long[]{rs.getLong(1), rs.getLong(2)}, commentId);
        if (r.isEmpty()) throw new NotFound();
        card(me, r.get(0)[0]);   // 같은 가족·볼 수 있는 글인지
        if (r.get(0)[1] != me.memberId() && !me.has(Perm.COMMENT_DELETE_ANY)) throw new Forbidden();
        jdbc.update("UPDATE comment SET deleted_at=?, deleted_by=? WHERE id=?", Times.now(), me.memberId(), commentId);
    }

    /** 하트 토글 → {liked, count, names} */
    public Map<String, Object> toggleLike(Me me, long postId) {
        card(me, postId);
        boolean liked = tx.execute(s -> {
            if (jdbc.update("DELETE FROM reaction WHERE post_id=? AND member_id=?", postId, me.memberId()) > 0) return false;
            jdbc.update("INSERT INTO reaction(post_id, member_id, created_at) VALUES (?,?,?)", postId, me.memberId(), Times.now());
            return true;
        });
        List<String> names = jdbc.queryForList("SELECT m.name FROM reaction r JOIN member m ON m.id=r.member_id WHERE r.post_id=? ORDER BY r.created_at", String.class, postId);
        return Map.of("liked", liked, "count", names.size(), "names", names);
    }

    /** 앨범: 볼 수 있는 글의 사진, 촬영일(없으면 글 날짜) 최신순 */
    public List<Media> album(Me me, int page, int size) {
        return jdbc.query("""
                SELECT md.id, md.post_id, md.member_id, md.file, md.width, md.height, COALESCE(md.taken_at, p.post_date) AS d
                FROM media md JOIN post p ON p.id = md.post_id
                WHERE md.kind='photo' AND md.family_id=? AND\s""" + VISIBLE + """
                 ORDER BY d DESC, md.id DESC LIMIT ? OFFSET ?""",
                (rs, i) -> media.map(me.familyId(), rs.getLong(1), rs.getLong(2), rs.getLong(3), rs.getString(4),
                        Rs.integer(rs, 5), Rs.integer(rs, 6), rs.getString(7)),
                me.familyId(), me.familyId(), me.memberId(), size + 1, Math.max(0, page) * size);
    }
}
