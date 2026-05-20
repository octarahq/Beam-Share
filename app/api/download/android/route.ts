import { NextResponse } from 'next/server';

export async function GET() {
  try {
    const res = await fetch('https://api.github.com/repos/octarahq/Beam-Share/releases/latest', {
      next: { revalidate: 3600 },
    });
    
    if (!res.ok) {
      return NextResponse.redirect('https://github.com/octarahq/Beam-Share/releases/latest');
    }

    const data = await res.json();
    const apkAsset = data.assets?.find((asset: any) => asset.name.endsWith('.apk'));

    if (apkAsset && apkAsset.browser_download_url) {
      return NextResponse.redirect(apkAsset.browser_download_url);
    }

    return NextResponse.redirect('https://github.com/octarahq/Beam-Share/releases/latest');
  } catch (error) {
    return NextResponse.redirect('https://github.com/octarahq/Beam-Share/releases/latest');
  }
}
