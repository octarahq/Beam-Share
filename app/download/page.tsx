import React from "react";
import Footer from "../components/Footer";

export default function Download() {
  return (
    <div className="bg-background text-on-surface font-body-md selection:bg-secondary selection:text-on-secondary min-h-screen">
      <main className="flex-grow pt-32 pb-20 px-margin-mobile md:px-margin-desktop max-w-container-max mx-auto w-full relative">
        <div className="absolute top-0 right-0 -z-10 w-[500px] h-[500px] bg-primary/10 blur-[120px] rounded-full"></div>
        <div className="absolute bottom-0 left-0 -z-10 w-[400px] h-[400px] bg-secondary/5 blur-[100px] rounded-full"></div>
        <section className="text-center mb-16 max-w-3xl mx-auto">
          <h1 className="font-display-lg text-display-lg mb-6 leading-tight">
            Light speed for your files.
          </h1>
          <p className="font-body-lg text-body-lg text-on-surface-variant mb-4">
            Download our 100% free client for everyone. No limits, no
            subscriptions, just ultra-fast sharing.
          </p>
          <div className="h-1 w-24 mx-auto transfer-pulse rounded-full opacity-60"></div>
        </section>
        <div className="grid grid-cols-1 md:grid-cols-12 gap-6 items-stretch">
          <div className="md:col-span-8 group cursor-pointer">
            <div className="bg-[#3730a3]/10 backdrop-blur-[20px] border border-white/10 border-t-secondary/30 border-l-secondary/30 shadow-[0_20px_50px_rgba(55,48,163,0.25)] rounded-3xl p-8 h-full flex flex-col md:flex-row justify-between items-center gap-8 hover:scale-[1.01] transition-all duration-500 hover:-translate-y-2 hover:shadow-[0_20px_50px_rgba(55,48,163,0.25)] hover:border-secondary/30 relative overflow-hidden group">
              <div className="z-10 text-center md:text-left">
                <div className="flex items-center gap-3 mb-4 justify-center md:justify-start">
                  <span className="material-symbols-outlined text-4xl text-secondary transition-transform duration-500 group-hover:scale-110">
                    desktop_windows
                  </span>
                  <h2 className="font-headline-lg text-headline-lg">Windows</h2>
                </div>
                <p className="font-body-md text-body-md text-on-surface-variant mb-8 max-w-sm">
                  Enjoy BeamShare on your PC with transfer
                  speeds optimized for the local network.
                </p>
                <div className="flex flex-wrap gap-4 justify-center md:justify-start">
                  {/* <button className="bg-secondary text-on-secondary px-6 py-3 rounded-xl font-label-md text-label-md font-bold flex items-center gap-2 hover:shadow-[0_0_20px_rgba(6,182,212,0.4)] transition-shadow duration-300 transition-all">
                    <span className="material-symbols-outlined">download</span>{" "}
                    Download .exe
                  </button> */}
                  <button className="bg-surface-container-high text-on-surface px-6 py-3 rounded-xl font-label-md text-label-md font-bold flex items-center gap-2 border border-outline-variant hover:border-secondary/50 transition-all">
                    <span className="material-symbols-outlined">pending</span>{" "}
                    Currently in development
                  </button>
                </div>
              </div>
            </div>
          </div>
          <div className="md:col-span-4 group cursor-pointer">
            <div className="bg-[#3730a3]/10 backdrop-blur-[20px] border border-white/10 border-t-secondary/30 border-l-secondary/30 rounded-3xl p-8 h-full flex flex-col hover:scale-[1.01] transition-all duration-500 hover:-translate-y-2 hover:shadow-[0_20px_50px_rgba(55,48,163,0.25)] hover:border-secondary/30 relative overflow-hidden group">
              <div className="mb-8">
                <div className="flex justify-between items-start mb-4">
                  <span className="material-symbols-outlined text-4xl text-secondary transition-transform duration-500 group-hover:scale-110">
                    smartphone
                  </span>
                </div>
                <h2 className="font-headline-md text-headline-md">Android</h2>
                <p className="font-body-md text-body-md text-on-surface-variant mt-2">
                  Share your photos and videos instantly.
                </p>
              </div>
              <div className="mt-auto space-y-3">
                <button className="w-full bg-secondary text-on-secondary px-6 py-3 rounded-xl font-label-md text-label-md font-bold flex items-center justify-center gap-2 transition-all">
                  <span
                    className="material-symbols-outlined"
                    style={{ fontVariationSettings: `'FILL' 1` }}
                  >
                    play_arrow
                  </span>{" "}
                  Google Play Store
                </button>
                <a href="/api/download/android" className="w-full bg-white/5 text-on-surface px-6 py-3 rounded-xl font-label-md text-label-md flex items-center justify-center gap-2 border border-outline-variant transition-all hover:bg-white/10">
                  <span className="material-symbols-outlined">
                    install_mobile
                  </span>{" "}
                  Download APK
                </a>
              </div>
            </div>
          </div>
          <div className="md:col-span-4 group cursor-pointer">
            <div className="bg-[#3730a3]/10 backdrop-blur-[20px] border border-white/10 border-t-secondary/30 border-l-secondary/30 rounded-3xl p-8 h-full flex flex-col hover:scale-[1.01] transition-all duration-500 hover:-translate-y-2 hover:shadow-[0_20px_50px_rgba(55,48,163,0.25)] hover:border-secondary/30 relative overflow-hidden group">
              <div className="mb-8">
                <span className="material-symbols-outlined text-4xl text-secondary mb-4 transition-transform duration-500 group-hover:scale-110">
                  phone_iphone
                </span>
                <h2 className="font-headline-md text-headline-md">iOS</h2>
                <p className="font-body-md text-body-md text-on-surface-variant mt-2">
                  The native iPhone &amp; iPad experience for your files.
                </p>
              </div>
              <div className="mt-auto">
                <button className="w-full bg-secondary text-on-secondary px-6 py-3 rounded-xl font-label-md text-label-md font-bold flex items-center justify-center gap-2 transition-all">
                  <span
                    className="material-symbols-outlined"
                    style={{ fontVariationSettings: `'FILL' 1` }}
                  >
                    pending
                  </span>{" "}
                  Currently in development
                </button>
              </div>
            </div>
          </div>
          <div className="md:col-span-4 group cursor-pointer">
            <div className="bg-[#3730a3]/10 backdrop-blur-[20px] border border-white/10 border-t-secondary/30 border-l-secondary/30 rounded-3xl p-8 h-full flex flex-col hover:scale-[1.01] transition-all duration-500 hover:-translate-y-2 hover:shadow-[0_20px_50px_rgba(55,48,163,0.25)] hover:border-secondary/30 relative overflow-hidden group">
              <div className="mb-8">
                <span className="material-symbols-outlined text-4xl text-secondary mb-4 transition-transform duration-500 group-hover:scale-110">
                  laptop_mac
                </span>
                <h2 className="font-headline-md text-headline-md">macOS</h2>
                <p className="font-body-md text-body-md text-on-surface-variant mt-2">
                  Seamless integration with Finder and the menu bar.
                </p>
              </div>
              <div className="mt-auto space-y-3">
                <button className="w-full bg-surface-container-high text-on-surface px-6 py-3 rounded-xl font-label-md text-label-md font-bold flex items-center justify-center gap-2 border border-outline-variant transition-all">
                  <span
                    className="material-symbols-outlined"
                    style={{ fontVariationSettings: `'FILL' 1` }}
                  >
                    pending
                  </span>{" "}
                  Currently in development
                </button>
              </div>
            </div>
          </div>
          <div className="md:col-span-4 group cursor-pointer">
            <div className="bg-[#3730a3]/10 backdrop-blur-[20px] border border-white/10 border-t-secondary/30 border-l-secondary/30 rounded-3xl p-8 h-full flex flex-col hover:scale-[1.01] transition-all duration-500 hover:-translate-y-2 hover:shadow-[0_20px_50px_rgba(55,48,163,0.25)] hover:border-secondary/30 relative overflow-hidden group">
              <div className="mb-8">
                <div className="flex justify-between items-start mb-4">
                  <span className="material-symbols-outlined text-4xl text-secondary transition-transform duration-500 group-hover:scale-110">
                    terminal
                  </span>
                </div>
                <h2 className="font-headline-md text-headline-md">Linux</h2>
                <p className="font-body-md text-body-md text-on-surface-variant mt-2">
                  Available for all major distros.
                </p>
              </div>
              <div className="mt-auto flex flex-wrap gap-2">
                <button className="w-full mt-4 bg-surface-container-high text-on-surface px-6 py-3 rounded-xl font-label-md text-label-md font-bold flex items-center justify-center gap-2 border border-outline-variant transition-all">
                  <span className="material-symbols-outlined">pending</span> Currently in development
                </button>
              </div>
            </div>
          </div>
        </div>
        <section className="mt-24 text-center">
          <h3 className="font-headline-lg text-headline-lg mb-12">
            Why BeamShare?
          </h3>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-8 text-left">
            <div className="bg-[#3730a3]/10 backdrop-blur-[20px] border border-white/10 border-t-secondary/30 border-l-secondary/30 p-6 rounded-2xl border border-white/5 transition-all duration-500 hover:-translate-y-2 hover:shadow-[0_20px_50px_rgba(55,48,163,0.25)] hover:border-secondary/30 relative overflow-hidden group">
              <span className="material-symbols-outlined text-secondary text-3xl mb-4">
                bolt
              </span>
              <h4 className="font-headline-md text-headline-md mb-2">
                Direct P2P
              </h4>
              <p className="font-body-md text-body-md text-on-surface-variant">
                Direct transfers without the cloud for maximum
                speed.
              </p>
            </div>
            <div className="bg-[#3730a3]/10 backdrop-blur-[20px] border border-white/10 border-t-secondary/30 border-l-secondary/30 p-6 rounded-2xl border border-white/5 transition-all duration-500 hover:-translate-y-2 hover:shadow-[0_20px_50px_rgba(55,48,163,0.25)] hover:border-secondary/30 relative overflow-hidden group">
              <span className="material-symbols-outlined text-secondary text-3xl mb-4">
                lock
              </span>
              <h4 className="font-headline-md text-headline-md mb-2">
                AES-256 Encryption
              </h4>
              <p className="font-body-md text-body-md text-on-surface-variant">
                Your files remain private and secure end-to-end.
              </p>
            </div>
            <div className="bg-[#3730a3]/10 backdrop-blur-[20px] border border-white/10 border-t-secondary/30 border-l-secondary/30 p-6 rounded-2xl border border-white/5 transition-all duration-500 hover:-translate-y-2 hover:shadow-[0_20px_50px_rgba(55,48,163,0.25)] hover:border-secondary/30 relative overflow-hidden group">
              <span className="material-symbols-outlined text-secondary text-3xl mb-4">
                sync
              </span>
              <h4 className="font-headline-md text-headline-md mb-2">
                Cross-platform
              </h4>
              <p className="font-body-md text-body-md text-on-surface-variant">
                Seamless transition between your mobile, tablet, and
                PC.
              </p>
            </div>
          </div>
        </section>
      </main>
      <Footer />
    </div>
  );
}
