"use client";

import React from "react";
import Link from "next/link";

export default function Footer() {
  return (
    <footer className="w-full py-12 px-margin-mobile md:px-margin-desktop max-w-container-max mx-auto border-t border-outline-variant dark:border-outline-variant bg-surface-container-lowest mt-20">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-8 mb-12">
          <div className="col-span-2 md:col-span-1">
            <div className="flex items-center gap-2 mb-6">
              <img alt="BeamShare Logo" className="w-8 h-8 object-contain" src="/favicon.ico"/>
              <span className="font-headline-md text-headline-md font-bold text-on-surface">BeamShare</span>
            </div>
            <p className="font-body-md text-body-md text-on-surface-variant">The open-source file transfer alternative, focused on privacy.</p>
          </div>
          <div>
            <h4 className="font-label-md text-label-md text-on-surface mb-6">Product</h4>
            <ul className="space-y-4">
              <li><a className="font-label-sm text-label-sm text-on-surface-variant hover:text-secondary transition-colors" href="/features">Features</a></li>
              <li><a className="font-label-sm text-label-sm text-on-surface-variant hover:text-secondary transition-colors" href="/download">Downloads</a></li>
              <li><a className="font-label-sm text-label-sm text-on-surface-variant hover:text-secondary transition-colors" href="/api/github" target="_blank">Source Code</a></li>
            </ul>
          </div>
          <div>
            <h4 className="font-label-md text-label-md text-on-surface mb-6">Community</h4>
            <ul className="space-y-4">
              <li><a className="font-label-sm text-label-sm text-on-surface-variant hover:text-secondary transition-colors" href="https://octara.xyz/" target="_blank">Octara</a></li>
            </ul>
          </div>
          <div>
            <h4 className="font-label-md text-label-md text-on-surface mb-6">Legal</h4>
            <ul className="space-y-4">
              <li><a className="font-label-sm text-label-sm text-on-surface-variant hover:text-secondary transition-colors" href="/privacy">Privacy Policy</a></li>
              <li><a className="font-label-sm text-label-sm text-on-surface-variant hover:text-secondary transition-colors" href="https://octara.xyz/terms" target="_blank">Terms</a></li>
            </ul>
          </div>
        </div>
        <div className="pt-8 border-t border-white/5 flex flex-col md:flex-row justify-between items-center gap-4">
          <p className="font-label-sm text-label-sm text-on-surface-variant">© {new Date().getFullYear()} Octara. GPL-3.0 LICENCE</p>
          <div className="flex gap-6">
            <a className="text-on-surface-variant hover:text-secondary transition-colors" href="https://octara.xyz/" target="_blank">
              <span className="material-symbols-outlined">language</span>
            </a>
            <button className="text-on-surface-variant hover:text-secondary transition-colors" onClick={(e) => { e.preventDefault(); navigator.clipboard.writeText(window.location.href); }}>
              <span className="material-symbols-outlined">content_copy</span>
            </button>
            <a className="text-on-surface-variant hover:text-secondary transition-colors" href="/api/github" target="_blank">
              <span className="material-symbols-outlined">code</span>
            </a>
          </div>
        </div>
      </footer>
  );
}
