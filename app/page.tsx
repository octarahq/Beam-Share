import React from "react";
import Footer from "./components/Footer";
import Link from "next/link";

export default function Home() {
  return (
    <div className="bg-background text-on-surface font-body-md selection:bg-secondary selection:text-on-secondary min-h-screen">
      <main className="relative">
        <section className="relative min-h-[70vh] flex items-center justify-center pt-24 overflow-hidden px-margin-mobile">
          <div className="container mx-auto text-center z-10">
            <h1 className="font-display-lg text-display-lg mb-6 max-w-4xl mx-auto tracking-tight leading-tight">
              Share without borders.
            </h1>
            <p className="font-body-lg text-body-lg text-on-surface-variant max-w-2xl mx-auto mb-10">
              The universal and free alternative to QuickShare, AirDrop, etc.
              Direct P2P transfers without the cloud. Fast,
              sovereign, and open-source.
            </p>
            <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
              <Link
                href="/download"
                className="w-full sm:w-auto bg-secondary text-on-secondary px-10 py-4 rounded-xl font-label-md text-headline-md font-bold transition-all hover:opacity-90 active:scale-95 hover:shadow-[0_0_20px_rgba(6,182,212,0.4)] transition-shadow duration-300"
              >
                Download for free
              </Link>
              <Link
                href="/api/github"
                target="_blank"
                className="w-full sm:w-auto bg-[#3730a3]/10 backdrop-blur-[20px] border border-white/10 border-t-secondary/30 border-l-secondary/30 text-on-surface px-10 py-4 rounded-xl font-label-md text-headline-md font-bold hover:bg-white/5 active:scale-95 flex items-center justify-center gap-2 transition-all duration-500 hover:-translate-y-2 hover:shadow-[0_20px_50px_rgba(55,48,163,0.25)] hover:border-secondary/30 relative overflow-hidden group"
              >
                <span className="material-symbols-outlined">terminal</span>
                Explore the code
              </Link>
            </div>
          </div>
        </section>
        <section className="py-24 bg-surface-container-lowest px-margin-mobile">
          <div className="max-w-container-max mx-auto">
            <div className="text-center mb-16">
              <h2 className="font-headline-lg text-headline-lg mb-4">
                How it works
              </h2>
              <p className="text-on-surface-variant font-body-md">
                Des transferts P2P directs entre vos appareils, sans
                intermédiaire.
              </p>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
              <div className="p-8 bg-[#3730a3]/10 backdrop-blur-[20px] border border-white/10 border-t-secondary/30 border-l-secondary/30 rounded-3xl transition-all duration-500 hover:-translate-y-2 hover:shadow-[0_20px_50px_rgba(55,48,163,0.25)] hover:border-secondary/30 relative overflow-hidden group">
                <div className="w-16 h-16 bg-primary-container rounded-2xl flex items-center justify-center mb-6 text-on-primary-container transition-transform duration-500 group-hover:scale-110">
                  <span className="material-symbols-outlined text-3xl">
                    launch
                  </span>
                </div>
                <h3 className="font-headline-md text-headline-md mb-3">
                  1. Open BeamShare
                </h3>
                <p className="text-on-surface-variant font-body-md">
                  Lancez l'application sur n'importe quel appareil. Pas de
                  compte requis, pas de collecte de données.
                </p>
              </div>
              <div className="p-8 bg-[#3730a3]/10 backdrop-blur-[20px] border border-white/10 border-t-secondary/30 border-l-secondary/30 rounded-3xl transition-all duration-500 hover:-translate-y-2 hover:shadow-[0_20px_50px_rgba(55,48,163,0.25)] hover:border-secondary/30 relative overflow-hidden group">
                <div className="w-16 h-16 bg-secondary-container rounded-2xl flex items-center justify-center mb-6 text-on-secondary-container transition-transform duration-500 group-hover:scale-110">
                  <span className="material-symbols-outlined text-3xl">
                    hub
                  </span>
                </div>
                <h3 className="font-headline-md text-headline-md mb-3">
                  2. Direct Connection
                </h3>
                <p className="text-on-surface-variant font-body-md">
                  BeamShare établit un tunnel local sécurisé directement entre
                  les deux appareils concernés.
                </p>
              </div>
              <div className="p-8 bg-[#3730a3]/10 backdrop-blur-[20px] border border-white/10 border-t-secondary/30 border-l-secondary/30 rounded-3xl transition-all duration-500 hover:-translate-y-2 hover:shadow-[0_20px_50px_rgba(55,48,163,0.25)] hover:border-secondary/30 relative overflow-hidden group">
                <div className="w-16 h-16 bg-surface-variant rounded-2xl flex items-center justify-center mb-6 text-primary transition-transform duration-500 group-hover:scale-110">
                  <span className="material-symbols-outlined text-3xl">
                    rocket_launch
                  </span>
                </div>
                <h3 className="font-headline-md text-headline-md mb-3">
                  3. Send without limits
                </h3>
                <p className="text-on-surface-variant font-body-md">
                  Transfer files of any size at the speed of your local network. Guaranteed 100% cloud-free.
                </p>
              </div>
            </div>
          </div>
        </section>
        <section className="py-24 px-margin-mobile" id="features">
          <div className="max-w-container-max mx-auto">
            <div className="mb-16 text-left">
              <h2 className="font-headline-lg text-headline-lg mb-4">
                Built for freedom
              </h2>
              <p className="text-on-surface-variant font-body-md">
                Transparent technology serving your privacy.
              </p>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-12 grid-rows-2 gap-6 h-auto md:h-[600px]">
              <div className="md:col-span-8 bg-[#3730a3]/10 backdrop-blur-[20px] border border-white/10 border-t-secondary/30 border-l-secondary/30 rounded-3xl p-10 flex flex-col justify-between transition-all duration-500 hover:-translate-y-2 hover:shadow-[0_20px_50px_rgba(55,48,163,0.25)] hover:border-secondary/30 relative overflow-hidden group">
                <div className="z-10">
                  <h3 className="font-headline-lg text-headline-lg mb-4">
                    Lightning fast speed
                  </h3>
                  <p className="text-on-surface-variant max-w-md font-body-md">
                    Uses the Peer-to-Peer protocol or local network depending
                    on the situation. No size, time, or other
                    limits.
                  </p>
                </div>
              </div>
              <div className="md:col-span-4 bg-[#3730a3]/10 backdrop-blur-[20px] border border-white/10 border-t-secondary/30 border-l-secondary/30 rounded-3xl p-10 flex flex-col justify-between transition-all duration-500 hover:-translate-y-2 hover:shadow-[0_20px_50px_rgba(55,48,163,0.25)] hover:border-secondary/30 relative overflow-hidden group">
                <div>
                  <h3 className="font-headline-md text-headline-md mb-4">
                    Cross-platform
                  </h3>
                  <p className="text-on-surface-variant font-body-md">
                    iOS, Android, Windows, Mac, Linux. A universal solution
                    to break proprietary silos.
                  </p>
                </div>
                <div className="flex flex-wrap gap-2 mt-6">
                  <span className="px-3 py-1 rounded-full bg-white/5 border border-white/10 text-label-sm">
                    Android
                  </span>
                  <span className="px-3 py-1 rounded-full bg-white/5 border border-white/10 text-label-sm">
                    Linux
                  </span>
                  <span className="px-3 py-1 rounded-full bg-white/5 border border-white/10 text-label-sm">
                    iOS
                  </span>
                  <span className="px-3 py-1 rounded-full bg-white/5 border border-white/10 text-label-sm">
                    macOS
                  </span>
                  <span className="px-3 py-1 rounded-full bg-white/5 border border-white/10 text-label-sm">
                    Windows
                  </span>
                </div>
              </div>
              <div className="md:col-span-4 bg-[#3730a3]/10 backdrop-blur-[20px] border border-white/10 border-t-secondary/30 border-l-secondary/30 rounded-3xl p-10 flex flex-col justify-between transition-all duration-500 hover:-translate-y-2 hover:shadow-[0_20px_50px_rgba(55,48,163,0.25)] hover:border-secondary/30 relative overflow-hidden group">
                <div>
                  <h3 className="font-headline-md text-headline-md mb-2">
                    Total Privacy
                  </h3>
                  <p className="text-on-surface-variant font-body-md">
                    End-to-end AES-256 encryption. Your data stays on
                    your devices, period.
                  </p>
                </div>
              </div>
              <div className="md:col-span-8 bg-[#3730a3]/10 backdrop-blur-[20px] border border-white/10 border-t-secondary/30 border-l-secondary/30 rounded-3xl p-10 flex flex-col md:flex-row gap-8 transition-all duration-500 hover:-translate-y-2 hover:shadow-[0_20px_50px_rgba(55,48,163,0.25)] hover:border-secondary/30 relative overflow-hidden group">
                <div className="flex-1">
                  <h3 className="font-headline-md text-headline-md mb-4">
                    Free and Open Source
                  </h3>
                  <p className="text-on-surface-variant font-body-md">
                    Auditable by everyone. We believe in an open web where
                    sharing should never compromise your digital
                    freedom.
                  </p>
                </div>
              </div>
            </div>
          </div>
        </section>
        <section className="py-24 px-margin-mobile text-center">
          <div className="max-w-4xl mx-auto bg-[#3730a3]/10 backdrop-blur-[20px] border border-white/10 border-t-secondary/30 border-l-secondary/30 p-12 md:p-20 rounded-[40px] border border-secondary/20 transition-all duration-500 hover:-translate-y-2 hover:shadow-[0_20px_50px_rgba(55,48,163,0.25)] hover:border-secondary/30 relative overflow-hidden group">
            <div className="absolute top-0 right-0 p-8 opacity-10">
              <span className="material-symbols-outlined text-[120px] text-secondary">
                share_off
              </span>
            </div>
            <h2 className="font-headline-lg text-display-lg mb-6 leading-tight">
              Ditch the cables, keep your data.
            </h2>
            <p className="font-body-lg text-body-lg text-on-surface-variant mb-10 max-w-2xl mx-auto">
              Join a community that values privacy and
              open-source for their daily transfers.
            </p>
            <div className="flex flex-col sm:flex-row items-center justify-center gap-6">
              <Link
                href="/download"
                className="w-full sm:w-auto bg-secondary text-on-secondary px-12 py-5 rounded-2xl font-headline-md text-headline-md font-bold transition-all hover:scale-105 hover:shadow-[0_0_20px_rgba(6,182,212,0.4)] transition-shadow duration-300"
              >
                Download for free
              </Link>
              <a
                className="text-on-surface-variant font-label-md border-b border-transparent hover:border-secondary hover:text-on-surface transition-all flex items-center gap-2"
                href="/api/github"
                target="_blank"
              >
                <span className="material-symbols-outlined text-sm">
                  terminal
                </span>{" "}
                Contribute on GitHub
              </a>
            </div>
          </div>
        </section>
      </main>
      <Footer />
    </div>
  );
}
