"use client";

import React, { useState, useEffect } from 'react';
import Link from 'next/link';

export default function Navbar() {
  const [isScrolled, setIsScrolled] = useState(false);
  const [isMenuOpen, setIsMenuOpen] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      setIsScrolled(window.scrollY > 50);
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <header className={`fixed top-0 w-full z-50 transition-all duration-300 border-b ${isScrolled ? 'bg-background/80 py-3 backdrop-blur-xl border-white/10' : 'bg-transparent py-4 border-transparent'}`}>
      <div className="max-w-container-max mx-auto px-6 md:px-margin-desktop flex justify-between items-center">
        <Link href="/" className="flex items-center gap-3">
          <img alt="BeamShare Logo" className="w-10 h-10 object-contain" src="/favicon.ico"/>
          <span className="font-headline-md text-headline-md font-bold text-on-surface">BeamShare</span>
        </Link>
        
        <nav className="hidden md:flex items-center gap-8">
          <Link className="font-label-md text-label-md text-on-surface-variant hover:text-on-surface transition-colors" href="/features">Fonctionnalités</Link>
          <Link className="font-label-md text-label-md text-on-surface-variant hover:text-on-surface transition-colors" href="/download">Download</Link>
          <a className="font-label-md text-label-md text-on-surface-variant hover:text-on-surface transition-colors" href="/api/github" target="_blank">Open Source</a>
        </nav>
        
        <div className="hidden md:flex items-center gap-4">
          
          <Link href="/download" className="bg-secondary text-on-secondary px-6 py-2.5 rounded-full font-label-md text-label-md font-bold transition-all hover:opacity-80 active:scale-95 hover:shadow-[0_0_20px_rgba(6,182,212,0.4)] transition-shadow duration-300">Download</Link>
        </div>

        <button 
          className="md:hidden flex flex-col gap-1.5 p-2"
          onClick={() => setIsMenuOpen(!isMenuOpen)}
        >
          <span className={`block w-6 h-0.5 bg-on-surface transition-all ${isMenuOpen ? 'rotate-45 translate-y-2' : ''}`}></span>
          <span className={`block w-6 h-0.5 bg-on-surface transition-all ${isMenuOpen ? 'opacity-0' : ''}`}></span>
          <span className={`block w-6 h-0.5 bg-on-surface transition-all ${isMenuOpen ? '-rotate-45 -translate-y-2' : ''}`}></span>
        </button>
      </div>

      <div className={`md:hidden absolute top-full left-0 w-full bg-background border-b border-white/10 transition-all duration-300 overflow-hidden ${isMenuOpen ? 'max-h-96 py-4' : 'max-h-0'}`}>
        <nav className="flex flex-col items-center gap-6">
          <Link onClick={() => setIsMenuOpen(false)} className="font-label-md text-label-md text-on-surface" href="/features">Fonctionnalités</Link>
          <Link onClick={() => setIsMenuOpen(false)} className="font-label-md text-label-md text-on-surface" href="/download">Download</Link>
          <a onClick={() => setIsMenuOpen(false)} className="font-label-md text-label-md text-on-surface" href="/api/github" target="_blank">Open Source</a>
          <Link href="/download" onClick={() => setIsMenuOpen(false)} className="bg-secondary text-on-secondary px-6 py-2.5 rounded-full font-label-md text-label-md font-bold">Download</Link>
        </nav>
      </div>
    </header>
  );
}
