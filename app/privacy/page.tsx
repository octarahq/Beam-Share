import React from "react";
import Footer from "../components/Footer";

const sections = [
  {
    title: "1. Data Collection and Transmission",
    content:
      "BeamShare is a decentralized, peer-to-peer file transfer application. We do not use any servers to facilitate transfers or store data. No data is ever sent to Octara or any third-party analytics/advertising services. All communication happens directly between your device and the recipient's device on your local network or via direct link.",
    icon: "cloud_off",
  },
  {
    title: "2. Data Stored on Your Device",
    content:
      "The application stores the following information locally in its private storage: ECDSA identity keys, device name, visibility and theme preferences, transfer history (last 50 items), and your selected download folder path.",
    icon: "storage",
  },
  {
    title: "3. Data Exchanged During Transfers",
    content:
      "When you discover or transfer files, the other party sees: your device name, model, and Public Key. File metadata (name, size, type) and cryptographic signatures are also exchanged. The actual file content is encrypted end-to-end using AES-256-GCM.",
    icon: "encrypted",
  },
  {
    title: "4. Permissions",
    content:
      "Internet and Wi-Fi permissions are used for local discovery and transmission. Bluetooth and Location (on some Android versions) are required for mDNS/BLE discovery. Notifications inform you of transfer requests.",
    icon: "admin_panel_settings",
  },
];

export const metadata = {
  title: "Privacy Policy — BeamShare",
  description:
    "BeamShare is a fully decentralized, serverless file transfer app. No data is collected, stored remotely, or shared with third parties.",
};

export default function Privacy() {
  return (
    <div className="bg-background text-on-surface font-body-md selection:bg-secondary selection:text-on-secondary min-h-screen">
      <main className="pt-32 pb-20 px-margin-mobile md:px-margin-desktop max-w-container-max mx-auto">
        <div className="max-w-3xl mx-auto">
          <div className="mb-12">
            <h1 className="font-display-lg text-display-lg leading-tight mb-4">
              Privacy Policy
            </h1>
            <p className="font-body-lg text-body-lg text-on-surface-variant">
              BeamShare is built on a simple principle: your files are yours. We
              have designed this application so that no data ever leaves your
              device without your explicit action.
            </p>
            <div className="mt-6 flex items-center gap-2 text-on-surface-variant font-body-sm text-body-sm">
              <span className="material-symbols-outlined text-base">
                schedule
              </span>
              Last updated: May 2025
            </div>
          </div>

          <div className="space-y-4">
            {sections.map((section, i) => (
              <div
                key={i}
                className="group bg-[#3730a3]/10 backdrop-blur-[20px] border border-white/10 border-t-secondary/30 border-l-secondary/30 rounded-3xl p-8 transition-all duration-500 hover:-translate-y-1 hover:shadow-[0_20px_50px_rgba(55,48,163,0.25)] hover:border-secondary/30 relative overflow-hidden"
              >
                <div className="absolute top-0 right-0 w-48 h-48 bg-primary/5 rounded-full blur-3xl -mr-12 -mt-12 group-hover:bg-primary/10 transition-all" />
                <div className="flex items-start gap-4 relative z-10">
                  <div className="shrink-0 w-12 h-12 rounded-2xl bg-secondary/10 border border-secondary/20 flex items-center justify-center group-hover:bg-secondary/20 transition-all duration-300">
                    <span className="material-symbols-outlined text-secondary text-xl">
                      {section.icon}
                    </span>
                  </div>
                  <div>
                    <h2 className="font-headline-sm text-headline-sm mb-3">
                      {section.title}
                    </h2>
                    <p className="font-body-md text-body-md text-on-surface-variant leading-relaxed">
                      {section.content}
                    </p>
                  </div>
                </div>
              </div>
            ))}
          </div>

          <div className="mt-10 bg-secondary/10 border border-secondary/20 rounded-3xl p-8 flex items-start gap-4">
            <span className="material-symbols-outlined text-secondary text-3xl shrink-0 mt-1">
              verified_user
            </span>
            <div>
              <h3 className="font-title-lg text-title-lg mb-2">
                Open Source & Auditable
              </h3>
              <p className="font-body-md text-body-md text-on-surface-variant leading-relaxed">
                BeamShare is fully open source under the GPL-3.0 license. You
                can audit every line of code on GitHub to verify these
                commitments for yourself. We believe trust is earned through
                transparency, not promises.
              </p>
              <a
                href="/api/github"
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-2 mt-4 text-secondary font-label-md text-label-md hover:underline"
              >
                <span className="material-symbols-outlined text-base">
                  open_in_new
                </span>
                View source code on GitHub
              </a>
            </div>
          </div>

          <div className="mt-8 text-center text-on-surface-variant font-body-sm text-body-sm">
            Questions? Contact us at{" "}
            <a
              href="https://octara.xyz"
              target="_blank"
              rel="noopener noreferrer"
              className="text-secondary hover:underline"
            >
              octara.xyz
            </a>
          </div>
        </div>
      </main>
      <Footer />
    </div>
  );
}
