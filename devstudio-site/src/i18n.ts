// 다국어: 한국어는 / , 영어는 /en/... , 태국어는 /th/...
// 한국어 문구는 site.json 이 원본. en/th 는 아래에서 덮어쓴다.
import site from './data/site.json';

export const langs = ['ko', 'en', 'th'] as const;
export type Lang = (typeof langs)[number];
export const langLabels: Record<Lang, string> = { ko: 'KO', en: 'EN', th: 'TH' };

// [...lang] 페이지 공통 getStaticPaths
export const langPaths = () => langs.map((l) => ({ params: { lang: l === 'ko' ? undefined : l } }));

export const getLang = (url: URL): Lang => {
  const seg = url.pathname.split('/')[1] as Lang;
  return langs.includes(seg) && seg !== 'ko' ? seg : 'ko';
};

// 언어 접두어 없는 경로(/work) → 해당 언어 경로(/en/work)
export const href = (lang: Lang, path: string) => (lang === 'ko' ? path : `/${lang}${path === '/' ? '' : path}`);
// 현재 경로에서 언어 접두어 제거
export const stripLang = (pathname: string) => pathname.replace(/^\/(en|th)(?=\/|$)/, '') || '/';

const content = {
  en: {
    tagline: 'Product & food photography studio',
    heroText: 'Product & food photography studio',
    about: 'We capture the value of your products and complete your brand image.',
    services: [
      { category: 'COSMETICS', text: 'Cosmetics & beauty styling shots, detail-page photography' },
      { category: 'FOOD', text: 'Menu, food & beverage styling photography' },
    ],
    process: [
      { title: 'Inquiry', text: 'Tell us what you need and when by phone or KakaoTalk.' },
      { title: 'Quote & Planning', text: 'We send a quote and shot plan within 24 hours.' },
      { title: 'Shoot', text: 'We follow the timetable and review shots with you on set.' },
      { title: 'Retouch & Delivery', text: 'After selection, retouched images are delivered on the agreed date.' },
    ],
    reviews: [
      { text: 'They worked through the planning with us, and the results exceeded our expectations.', author: 'Cosmetics brand CEO' },
      { text: 'The shoot day ran smoothly and delivery arrived on the promised date.', author: 'Fashion brand marketer' },
      { text: 'It was my first profile shoot and I was nervous, but they made me feel at ease.', author: 'Individual client' },
    ],
    faq: [
      { q: 'How is the price determined?', a: 'It depends on the number of shots, styling complexity and retouching scope. Message us on KakaoTalk for a quote.' },
      { q: 'How many shots can you do in a day?', a: 'It varies with styling complexity, but we usually plan about 30 minutes per shot.' },
      { q: 'How is the schedule confirmed?', a: 'We confirm the shoot date after a consultation by phone or KakaoTalk.' },
      { q: 'How long does delivery take?', a: 'Retouched images are usually delivered within 5–7 business days after selection.' },
      { q: 'Can I inquire without a plan?', a: 'Yes. Just tell us the product and how it will be used, and we will build a plan together.' },
    ],
    contact: { address: site.contact.addressEn },
  },
  th: {
    tagline: 'สตูดิโอถ่ายภาพสินค้าและอาหาร',
    heroText: 'สตูดิโอถ่ายภาพสินค้าและอาหาร',
    about: 'เราถ่ายทอดคุณค่าของสินค้า และเติมเต็มภาพลักษณ์ของแบรนด์คุณ',
    services: [
      { category: 'COSMETICS', text: 'ภาพจัดวางเครื่องสำอางและบิวตี้ ภาพสำหรับหน้ารายละเอียดสินค้า' },
      { category: 'FOOD', text: 'ถ่ายภาพจัดวางเมนู อาหาร และเครื่องดื่ม' },
    ],
    process: [
      { title: 'สอบถาม', text: 'แจ้งรายละเอียดงานและวันที่ต้องการทางโทรศัพท์หรือ KakaoTalk' },
      { title: 'ใบเสนอราคาและวางแผน', text: 'เราจะส่งใบเสนอราคาและแผนการถ่ายภาพภายใน 24 ชั่วโมง' },
      { title: 'ถ่ายภาพ', text: 'ดำเนินการตามตารางเวลา และตรวจดูภาพร่วมกันที่หน้างาน' },
      { title: 'รีทัชและส่งงาน', text: 'หลังคัดเลือกภาพ เราจะส่งภาพรีทัชตามวันที่ตกลงไว้' },
    ],
    reviews: [
      { text: 'ช่วยคิดตั้งแต่ขั้นวางแผน ผลงานออกมาเกินความคาดหมาย', author: 'ซีอีโอแบรนด์เครื่องสำอาง' },
      { text: 'วันถ่ายงานเป็นไปอย่างราบรื่น และส่งงานตรงตามวันที่สัญญา', author: 'นักการตลาดแบรนด์เสื้อผ้า' },
      { text: 'ถ่ายโปรไฟล์ครั้งแรกเลยตื่นเต้น แต่ทีมงานช่วยให้รู้สึกสบายใจ', author: 'ลูกค้าบุคคล' },
    ],
    faq: [
      { q: 'ค่าถ่ายภาพคิดอย่างไร?', a: 'ขึ้นอยู่กับจำนวนภาพ ความยากของการจัดวาง และขอบเขตการรีทัช ติดต่อเราทาง KakaoTalk เพื่อขอใบเสนอราคา' },
      { q: 'ถ่ายได้กี่ภาพต่อวัน?', a: 'ขึ้นอยู่กับความยากของการจัดวาง โดยทั่วไปเราคิดประมาณ 30 นาทีต่อภาพ' },
      { q: 'ยืนยันวันถ่ายอย่างไร?', a: 'เรายืนยันวันถ่ายหลังจากปรึกษาทางโทรศัพท์หรือ KakaoTalk' },
      { q: 'ส่งงานใช้เวลานานเท่าไร?', a: 'โดยปกติส่งภาพรีทัชภายใน 5–7 วันทำการหลังคัดเลือกภาพ' },
      { q: 'ยังไม่มีแผนงาน สอบถามได้ไหม?', a: 'ได้เลย แค่บอกสินค้าและการนำไปใช้ เราจะช่วยวางแผนให้' },
    ],
    contact: { address: site.contact.addressEn },
  },
};

export const getSite = (lang: Lang) =>
  lang === 'ko' ? site : { ...site, ...content[lang], contact: { ...site.contact, ...content[lang].contact } };

const ui = {
  ko: {
    menu: '메뉴', kakaoFab: '카톡 문의',
    ctaInquiry: '촬영 문의하기 →', ctaPortfolio: '포트폴리오 보기 →', clients: '함께한 브랜드',
    viewAll: '전체 보기 →', coverAlt: '대표 사진', viewWork: '작업 보기 →',
    ctaTitle: '촬영 계획이 있으신가요?', ctaSub: '24시간 안에 견적과 촬영 구성안을 보내드립니다.',
    ctaKakao: '카카오톡으로 문의하기 →', ctaOther: '다른 연락 방법 보기',
    portfolio: '포트폴리오', contactDesc: '촬영 문의',
    contactLead: '24시간 안에 답장드립니다. 예산과 일정을 알려주시면 구성 근거와 함께 안내해 드려요.',
    checklist: ['이름 / 회사명', '촬영 종류 (제품·푸드 등)', '희망 촬영일', '예산', '레퍼런스 사진이나 링크'],
    chatTitle: '카카오톡 오픈채팅으로<br />편하게 문의하세요', chatLabel: '문의할 때 아래 내용을 함께 보내주시면 더 빨라요',
    chatBtn: '카카오톡 오픈채팅 문의하기 →', schedule: '(SCHEDULE — 예약 가능 일정)', scheduleTitle: '촬영 가능 일정',
    photoAlt: '촬영 사진', lbLabel: '사진 크게 보기', lbClose: '닫기', lbPrev: '이전 사진', lbNext: '다음 사진',
  },
  en: {
    menu: 'Menu', kakaoFab: 'KakaoTalk',
    ctaInquiry: 'Book a shoot →', ctaPortfolio: 'View portfolio →', clients: 'Brands we worked with',
    viewAll: 'View all →', coverAlt: 'cover photo', viewWork: 'View work →',
    ctaTitle: 'Planning a shoot?', ctaSub: 'We send a quote and shot plan within 24 hours.',
    ctaKakao: 'Contact us on KakaoTalk →', ctaOther: 'Other ways to reach us',
    portfolio: 'Portfolio', contactDesc: 'Photography inquiry',
    contactLead: 'We reply within 24 hours. Share your budget and schedule, and we will suggest a plan with our reasoning.',
    checklist: ['Name / Company', 'Type of shoot (product, food, etc.)', 'Preferred date', 'Budget', 'Reference photos or links'],
    chatTitle: 'Reach us easily on<br />KakaoTalk Open Chat', chatLabel: 'Including the following speeds things up',
    chatBtn: 'Chat on KakaoTalk →', schedule: '(SCHEDULE — AVAILABLE DATES)', scheduleTitle: 'Available dates',
    photoAlt: 'photo', lbLabel: 'Photo viewer', lbClose: 'Close', lbPrev: 'Previous photo', lbNext: 'Next photo',
  },
  th: {
    menu: 'เมนู', kakaoFab: 'ติดต่อ KakaoTalk',
    ctaInquiry: 'สอบถามการถ่ายภาพ →', ctaPortfolio: 'ดูผลงาน →', clients: 'แบรนด์ที่เคยร่วมงาน',
    viewAll: 'ดูทั้งหมด →', coverAlt: 'ภาพปก', viewWork: 'ดูผลงาน →',
    ctaTitle: 'มีแผนถ่ายภาพอยู่หรือเปล่า?', ctaSub: 'เราจะส่งใบเสนอราคาและแผนการถ่ายภาพภายใน 24 ชั่วโมง',
    ctaKakao: 'ติดต่อทาง KakaoTalk →', ctaOther: 'ช่องทางติดต่ออื่น',
    portfolio: 'ผลงาน', contactDesc: 'สอบถามการถ่ายภาพ',
    contactLead: 'เราตอบกลับภายใน 24 ชั่วโมง แจ้งงบประมาณและวันที่ เราจะเสนอแผนงานพร้อมเหตุผลให้',
    checklist: ['ชื่อ / บริษัท', 'ประเภทงาน (สินค้า อาหาร ฯลฯ)', 'วันที่ต้องการถ่าย', 'งบประมาณ', 'ภาพหรือลิงก์อ้างอิง'],
    chatTitle: 'ติดต่อเราได้ง่ายๆ<br />ผ่าน KakaoTalk Open Chat', chatLabel: 'ส่งข้อมูลด้านล่างมาด้วยจะรวดเร็วขึ้น',
    chatBtn: 'แชทผ่าน KakaoTalk →', schedule: '(SCHEDULE — วันที่ว่าง)', scheduleTitle: 'วันที่ว่างสำหรับถ่ายภาพ',
    photoAlt: 'ภาพถ่าย', lbLabel: 'ดูภาพขนาดใหญ่', lbClose: 'ปิด', lbPrev: 'ภาพก่อนหน้า', lbNext: 'ภาพถัดไป',
  },
} satisfies Record<Lang, Record<string, unknown>>;

export const t = (lang: Lang) => ui[lang];
