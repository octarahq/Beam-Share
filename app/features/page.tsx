import React from "react";
import Footer from "../components/Footer";

export default function Features() {
  return (
    <div className="bg-background text-on-surface font-body-md selection:bg-secondary selection:text-on-secondary min-h-screen">
      <main className="pt-32 pb-20 px-margin-mobile md:px-margin-desktop max-w-container-max mx-auto">
        <div className="grid grid-cols-1 md:grid-cols-12 gap-6">
          <div className="md:col-span-8 rounded-3xl p-8 md:p-12 bg-[#3730a3]/10 backdrop-blur-[20px] border border-white/10 border-t-secondary/30 border-l-secondary/30 transition-all duration-500 hover:-translate-y-2 hover:shadow-[0_20px_50px_rgba(55,48,163,0.25)] hover:border-secondary/30 relative overflow-hidden group">
            <div className="absolute top-0 right-0 w-64 h-64 bg-primary/10 rounded-full blur-3xl -mr-20 -mt-20 group-hover:bg-primary/20 transition-all"></div>
            <div className="relative z-10">
              <div className="w-14 h-14 rounded-2xl bg-primary-container flex items-center justify-center mb-8 transition-transform duration-500 group-hover:scale-110">
                <span
                  className="material-symbols-outlined text-white text-3xl"
                  style={{ fontVariationSettings: `'FILL' 1` }}
                >
                  dynamic_form
                </span>
              </div>
              <h2 className="font-headline-lg text-headline-lg mb-4">
                Transfert Direct P2P
              </h2>
              <p className="font-body-md text-body-md text-on-surface-variant max-w-md mb-8">
                Your files never pass through a third-party server. The
                connection is established directly between your devices via your
                local network for maximum speed and security.
              </p>
            </div>
          </div>
          <div className="md:col-span-4 rounded-3xl p-8 flex flex-col justify-between bg-[#3730a3]/10 backdrop-blur-[20px] border border-white/10 border-t-secondary/30 border-l-secondary/30 transition-all duration-500 hover:-translate-y-2 hover:shadow-[0_20px_50px_rgba(55,48,163,0.25)] hover:border-secondary/30 relative overflow-hidden group">
            <div>
              <div className="w-14 h-14 rounded-2xl bg-tertiary-container flex items-center justify-center mb-8 transition-transform duration-500 group-hover:scale-110">
                <span
                  className="material-symbols-outlined text-white text-3xl"
                  style={{ fontVariationSettings: `'FILL' 1` }}
                >
                  privacy_tip
                </span>
              </div>
              <h2 className="font-headline-lg text-headline-lg mb-4">
                Privacy by Design
              </h2>
              <p className="font-body-md text-body-md text-on-surface-variant">
                Zero data collection. No account required. End-to-end
                AES-256 encryption enabled by default for every transfer.
              </p>
            </div>
          </div>
          <div className="md:col-span-4 rounded-3xl p-8 bg-[#3730a3]/10 backdrop-blur-[20px] border border-white/10 border-t-secondary/30 border-l-secondary/30 transition-all duration-500 hover:-translate-y-2 hover:shadow-[0_20px_50px_rgba(55,48,163,0.25)] hover:border-secondary/30 relative overflow-hidden group">
            <div className="w-14 h-14 rounded-2xl bg-secondary-container flex items-center justify-center mb-8 transition-transform duration-500 group-hover:scale-110">
              <span className="material-symbols-outlined text-on-secondary-container text-3xl">
                terminal
              </span>
            </div>
            <h2 className="font-headline-lg text-headline-lg mb-4">
              Open Source Transparency
            </h2>
            <p className="font-body-md text-body-md text-on-surface-variant mb-8">
              The source code is entirely public. Audit, modify, or
              contribute to the project on GitHub. Trust is earned through
              transparency.
            </p>
          </div>
          <div className="md:col-span-8 rounded-3xl p-8 md:p-12 bg-[#3730a3]/10 backdrop-blur-[20px] border border-white/10 border-t-secondary/30 border-l-secondary/30 transition-all duration-500 hover:-translate-y-2 hover:shadow-[0_20px_50px_rgba(55,48,163,0.25)] hover:border-secondary/30 relative overflow-hidden group">
            <div className="flex flex-col md:flex-row gap-12 items-center">
              <div className="flex-1">
                <div className="w-14 h-14 rounded-2xl bg-surface-container-highest flex items-center justify-center mb-8 transition-transform duration-500 group-hover:scale-110">
                  <span className="material-symbols-outlined text-secondary text-3xl">
                    folder_shared
                  </span>
                </div>
                <h2 className="font-headline-lg text-headline-lg mb-4">
                  Local Management
                </h2>
                <p className="font-body-md text-body-md text-on-surface-variant">
                  Find your past transfers instantly. Your
                  history stays on your device, never synced
                  elsewhere.
                </p>
              </div>
              <div className="flex-1 w-full space-y-3">
                <div className="p-4 bg-background/40 rounded-2xl border border-white/5 flex items-center gap-4">
                  <div className="w-10 h-10 rounded-full bg-secondary/20 flex items-center justify-center">
                    <span className="material-symbols-outlined text-secondary text-sm">
                      arrow_downward
                    </span>
                  </div>
                  <div className="flex-1">
                    <p className="font-label-md text-label-md">
                      Vacation_Photos.zip
                    </p>
                    <p className="font-label-sm text-label-sm opacity-50">
                      100% • Local
                    </p>
                  </div>
                </div>
                <div className="p-4 bg-background/40 rounded-2xl border border-white/5 flex items-center gap-4 opacity-70">
                  <div className="w-10 h-10 rounded-full bg-primary/20 flex items-center justify-center">
                    <span className="material-symbols-outlined text-primary text-sm">
                      arrow_upward
                    </span>
                  </div>
                  <div className="flex-1">
                    <p className="font-label-md text-label-md">
                      Final_Project.pdf
                    </p>
                    <p className="font-label-sm text-label-sm opacity-50">
                      Transferred to "Laptop-Pro"
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        <section className="mt-24 rounded-[40px] bg-primary-container relative overflow-hidden p-12 md:p-24 text-center group">
          <div className="absolute inset-0 animated-gradient opacity-20"></div>
          <div className="relative z-10 max-w-2xl mx-auto">
            <h2 className="font-display-lg text-display-lg mb-8 text-white">
              Free, Fast, and Private
            </h2>
            <p className="font-body-lg text-body-lg text-on-primary-container mb-12">
              Join thousands of users who trust
              BeamShare for their daily exchanges. No subscription, no
              ads.
            </p>
            <div className="flex flex-col sm:flex-row justify-center gap-6">
              <button className="bg-secondary text-on-secondary px-10 py-5 rounded-full font-label-md text-label-md font-bold hover:scale-105 transition-all shadow-xl shadow-black/20 flex items-center justify-center gap-3">
                <span className="material-symbols-outlined">download</span>
                Download now
              </button>
              <button className="bg-white/10 backdrop-blur-md text-white border border-white/20 px-10 py-5 rounded-full font-label-md text-label-md font-bold hover:bg-white/20 transition-all flex items-center justify-center gap-3">
                <span className="material-symbols-outlined">code</span>
                Source Code
              </button>
            </div>
          </div>
          <div className="absolute bottom-0 left-1/2 -translate-x-1/2 w-[120%] h-64 bg-gradient-to-t from-background to-transparent opacity-50"></div>
        </section>
      </main>
      <Footer />
    </div>
  );
}
