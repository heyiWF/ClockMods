import { readableInk } from '../core/clock-themes';
import type { ThemeGlass } from '../core/prefs';
/** Same bounded black/white overlay as Ultimate GaussianGlass.brightnessTransform. */
export function glassBrightness(brightness: number): {
    alpha: number;
    white: boolean;
} {
    const value = Math.max(0, Math.min(100, brightness));
    return { alpha: Math.abs(value - 50) / 50 * .55, white: value > 50 };
}
/** The wallpaper remains untouched; CSS samples it only through card masks. */
export class GaussianGlass {
    private key = '';
    private constructor(private image: HTMLImageElement) { }
    static async load(url: string): Promise<GaussianGlass> {
        const image = new Image();
        image.src = url;
        await image.decode();
        return new GaussianGlass(image);
    }
    apply(root: HTMLElement, options: ThemeGlass): void {
        const width = root.clientWidth, height = root.clientHeight;
        const key = [width, height, options.strength, options.brightness].join('|');
        if (key === this.key)
            return;
        this.key = key;
        const { alpha, white } = glassBrightness(options.brightness);
        const scale = Math.max(width / this.image.naturalWidth, height / this.image.naturalHeight);
        const edge = Math.max(this.image.naturalWidth, this.image.naturalHeight) * scale;
        root.style.setProperty('--glass-image', 'url("' + this.image.src + '")');
        root.style.setProperty('--glass-width', width + 'px');
        root.style.setProperty('--glass-height', height + 'px');
        root.style.setProperty('--glass-blur', options.strength * .06 * edge / Math.min(192, Math.max(this.image.naturalWidth, this.image.naturalHeight)) + 'px');
        root.style.setProperty('--glass-overlay', white ? 'rgba(255,255,255,' + alpha + ')' : 'rgba(0,0,0,' + alpha + ')');
        const canvas = document.createElement('canvas');
        canvas.width = canvas.height = 3;
        const context = canvas.getContext('2d', { willReadFrequently: true });
        let tint = '#333333';
        if (context) {
            const cropW = width / scale, cropH = height / scale;
            context.drawImage(this.image, (this.image.naturalWidth - cropW) / 2, (this.image.naturalHeight - cropH) / 2, cropW, cropH, 0, 0, 3, 3);
            const pixels = context.getImageData(0, 0, 3, 3).data;
            const channels = [0, 1, 2].map(channel => { let sum = 0; for (let i = channel; i < pixels.length; i += 4)
                sum += pixels[i]; return Math.round(sum / 9 * (1 - alpha) + (white ? 255 * alpha : 0)); });
            tint = '#' + channels.map(value => value.toString(16).padStart(2, '0')).join('');
        }
        root.style.setProperty('--glass-ink', readableInk(tint));
    }
}
