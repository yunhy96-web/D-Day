// 원본 폴더 → 웹용 WebP 2종(썸네일·크게보기) + 포트폴리오 목록(src/data/portfolio.json). 원본은 읽기만 함.
// 구조: <분류>/<프로젝트명 순서번호>/<1.jpg, 2.jpg…>, <분류>/<대표사진.jpg>
// 프로젝트 순서 = 폴더 이름 끝 숫자, 사진 순서 = 파일 이름 숫자 순, 대표 사진 = 첫 번째 사진
// 실행: node scripts/optimize-photos.mjs   (이미 변환된 파일은 건너뜀, 쓰지 않는 변환본은 삭제)
import sharp from 'sharp';
import { readdirSync, statSync, mkdirSync, existsSync, writeFileSync, rmSync } from 'fs';

const CATEGORIES = ['COSMETICS', 'FOOD', 'PERSONAL WORK'];
const TITLES = { magazien: 'Magazine' };                       // 폴더명 오타·표기 보정
const OUT = 'public/photos';
const SIZES = { thumb: 800, large: 2400 };
const isPhoto = (f) => /\.jpe?g$/i.test(f);
const byNumber = (a, b) => a.localeCompare(b, undefined, { numeric: true });
const slug = (s) => s.toLowerCase().replace(/(\.jpe?g)+$/i, '').replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '');
const titleCase = (s) => TITLES[s] ?? s.replace(/\b\w/g, (c) => c.toUpperCase());

sharp.concurrency(2);
for (const k of Object.keys(SIZES)) mkdirSync(`${OUT}/${k}`, { recursive: true });

const projects = [];
const covers = [];
for (const category of CATEGORIES) {
  const dirs = readdirSync(category).filter((d) => statSync(`${category}/${d}`).isDirectory());
  const loose = readdirSync(category).filter(isPhoto);
  const groups = dirs.map((d) => {
    const [, name, order] = d.match(/^(.*?)\s*(\d+)?$/);
    return { dir: `${category}/${d}`, title: titleCase(name), order: Number(order ?? 999) };
  });
  // 브랜드 폴더가 있으면 분류 폴더 바로 아래 사진 = 분류 대표 사진, 없으면 분류 자체가 프로젝트
  if (loose.length && dirs.length) covers.push({ category, src: `${category}/${loose.sort(byNumber)[0]}`, name: `${slug(category)}-cover` });
  else if (loose.length) groups.push({ dir: category, title: titleCase(category.toLowerCase()), order: 0, loose: true });
  for (const g of groups.sort((a, b) => a.order - b.order)) {
    const id = slug(g.loose ? category : `${category} ${g.title}`);
    const files = readdirSync(g.dir).filter(isPhoto).sort(byNumber);
    const names = files.map((f) => `${id}-${slug(f) || 'photo'}`);
    if (new Set(names).size !== names.length) throw new Error(`파일 이름 중복: ${g.dir}`);
    projects.push({ id, title: g.title, category, photos: files.map((f, i) => ({ src: `${g.dir}/${f}`, name: names[i] })) });
  }
}

const all = [...projects.flatMap((p) => p.photos), ...covers];
let done = 0;
const convert = async (ph) => {
  for (const [k, px] of Object.entries(SIZES)) {
    const out = `${OUT}/${k}/${ph.name}.webp`;
    if (!existsSync(out)) {
      await sharp(ph.src, { limitInputPixels: false }).rotate()
        .resize(px, px, { fit: 'inside', withoutEnlargement: true })
        .webp({ quality: k === 'thumb' ? 78 : 82 }).toFile(out);
    }
  }
  const m = await sharp(`${OUT}/thumb/${ph.name}.webp`).metadata();
  Object.assign(ph, { w: m.width, h: m.height });
  if (++done % 25 === 0) console.log(`${done}/${all.length}`);
};
const queue = [...all];
await Promise.all(Array.from({ length: 6 }, async () => { while (queue.length) await convert(queue.shift()); }));

// 더 이상 쓰지 않는 변환본 정리
const keep = new Set(all.map((p) => `${p.name}.webp`));
let removed = 0;
for (const k of Object.keys(SIZES)) for (const f of readdirSync(`${OUT}/${k}`)) if (!keep.has(f)) { rmSync(`${OUT}/${k}/${f}`); removed++; }

for (const ph of all) delete ph.src;
writeFileSync('src/data/portfolio.json', JSON.stringify({ covers, projects }, null, 2) + '\n');
console.log(`done: ${projects.length} projects, ${covers.length} covers, ${all.length} photos, removed ${removed} old files`);
for (const p of projects) console.log(`  ${p.category.padEnd(13)} ${p.title.padEnd(18)} ${p.photos.length}장  ${p.id}`);
