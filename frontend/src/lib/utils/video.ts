export type VideoSource =
    | { type: 'youtube'; id: string; embedUrl: string; thumbUrl: string }
    | { type: 'file'; url: string };

export function parseVideo(raw: string): VideoSource {
    const url = raw.trim();
    try {
        const u = new URL(url);
        const host = u.hostname.replace(/^(www|m|music)\./, '');
        let id: string | null = null;

        if (host === 'youtu.be') {
            id = u.pathname.split('/')[1] || null;
        } else if (host === 'youtube.com') {
            if (u.pathname === '/watch') {
                id = u.searchParams.get('v');
            } else if (/^\/(embed|shorts|live)\//.test(u.pathname)) {
                id = u.pathname.split('/')[2] || null;
            }
        }

        if (id) {
            return {
                type: 'youtube',
                id,
                embedUrl: `https://www.youtube.com/embed/${id}`,
                thumbUrl: `https://i.ytimg.com/vi/${id}/hqdefault.jpg`,
            };
        }
    } catch {
        // không phải URL hợp lệ -> coi như file
    }
    return { type: 'file', url };
}