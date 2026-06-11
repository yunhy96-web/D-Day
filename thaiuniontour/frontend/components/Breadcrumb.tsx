import Link from 'next/link';

interface BreadcrumbProps {
  items: string[];
}

export default function Breadcrumb({ items }: BreadcrumbProps) {
  if (!items || items.length === 0) return null;

  return (
    <nav aria-label="breadcrumb" className="flex items-center gap-1 text-xs text-gray-500 flex-wrap">
      <Link href="/" className="hover:text-blue-600 transition-colors">
        홈
      </Link>
      {items.map((item, idx) => (
        <span key={idx} className="flex items-center gap-1">
          <svg className="h-3 w-3 text-gray-300 flex-shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
          </svg>
          <span className={idx === items.length - 1 ? 'text-gray-800 font-medium' : ''}>
            {item}
          </span>
        </span>
      ))}
    </nav>
  );
}
