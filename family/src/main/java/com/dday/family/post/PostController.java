package com.dday.family.post;

import com.dday.family.auth.Me;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PostController {
    private final PostService posts;

    public record CommentBody(String body) {}

    @PostMapping("/api/posts")
    public Map<String, Object> create(@RequestBody PostService.Write w, Me me) { return Map.of("id", posts.create(me, w)); }

    @PutMapping("/api/posts/{id}")
    public ResponseEntity<Void> update(@PathVariable long id, @RequestBody PostService.Write w, Me me) {
        posts.update(me, id, w);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/posts/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id, Me me) {
        posts.delete(me, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/posts/{id}/comments")
    public Map<String, Object> comment(@PathVariable long id, @RequestBody CommentBody b, Me me) { return Map.of("id", posts.comment(me, id, b.body())); }

    @DeleteMapping("/api/comments/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable long id, Me me) {
        posts.deleteComment(me, id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/posts/{id}/reaction")
    public Map<String, Object> like(@PathVariable long id, Me me) { return posts.toggleLike(me, id); }
}
