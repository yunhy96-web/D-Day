'use client';

import Link from 'next/link';
import { useState } from 'react';
import type { CategoryNode } from '@/lib/api';

interface MegaMenuProps {
  categories: CategoryNode[];
}

export default function MegaMenu({ categories }: MegaMenuProps) {
  const [openId, setOpenId] = useState<number | null>(null);

  if (categories.length === 0) return null;

  return (
    <nav className="hidden md:flex items-center gap-1">
      {categories.map((cat) => (
        <div
          key={cat.id}
          className="relative"
          onMouseEnter={() => setOpenId(cat.id)}
          onMouseLeave={() => setOpenId(null)}
        >
          <Link
            href={`/c/${cat.slug}`}
            className="flex items-center gap-1 px-3 py-2 text-sm font-medium text-gray-700 hover:text-blue-600 transition-colors rounded-md hover:bg-gray-50"
          >
            {cat.name}
            {cat.children && cat.children.length > 0 && (
              <svg className="h-3 w-3 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
              </svg>
            )}
          </Link>

          {openId === cat.id && cat.children && cat.children.length > 0 && (
            <div className="absolute left-0 top-full z-50 min-w-[200px] rounded-lg border border-gray-100 bg-white shadow-xl">
              <div className="py-2">
                {cat.children.map((child) => (
                  <div key={child.id}>
                    <Link
                      href={`/c/${child.slug}`}
                      className="flex items-center justify-between px-4 py-2 text-sm text-gray-700 hover:bg-blue-50 hover:text-blue-600"
                    >
                      {child.name}
                      {child.children && child.children.length > 0 && (
                        <svg className="h-3 w-3 text-gray-400" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
                        </svg>
                      )}
                    </Link>
                    {child.children && child.children.length > 0 && (
                      <div className="ml-4 border-l border-gray-100">
                        {child.children.map((grand) => (
                          <Link
                            key={grand.id}
                            href={`/c/${grand.slug}`}
                            className="block px-4 py-1.5 text-xs text-gray-500 hover:bg-blue-50 hover:text-blue-600"
                          >
                            {grand.name}
                          </Link>
                        ))}
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      ))}
    </nav>
  );
}
