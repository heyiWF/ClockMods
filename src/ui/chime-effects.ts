/** Drawing equations and palette ported from Ultimate RadialChimeView. */
const clamp = (value: number) => Math.max(0, Math.min(1, value));
export const easeOutQuint = (value: number) => 1 - (1 - value) ** 5;
export function smoothStep(start: number, end: number, value: number): number { const p = clamp((value - start) / (end - start)); return p * p * (3 - 2 * p); }
const rgba = (rgb: string, alpha: number) => 'rgba(' + rgb + ',' + clamp(alpha) + ')';
const colors = ['77,228,242', '167,139,250', '255,127,176'];
export function drawChimeEffect(c: CanvasRenderingContext2D, w: number, h: number, p: number, kind: string): void {
    const cx = w / 2, cy = h / 2, unit = Math.min(w, h), radius = Math.hypot(w, h) / 2, opacity = 1 - smoothStep(.82, 1, p);
    const circle = (x: number, y: number, r: number, color: string, stroke = 0) => { c.beginPath(); c.arc(x, y, Math.max(0, r), 0, Math.PI * 2); if (stroke) {
        c.strokeStyle = color;
        c.lineWidth = stroke;
        c.stroke();
    }
    else {
        c.fillStyle = color;
        c.fill();
    } };
    c.clearRect(0, 0, w, h);
    c.lineCap = 'round';
    if (kind === 'ripple' || kind === 'pulse') {
        c.fillStyle = rgba('244,196,48', opacity);
        c.fillRect(0, 0, w, h);
        if (kind === 'ripple')
            for (let i = 0; i < 3; i++) {
                const wave = easeOutQuint(clamp(p * 1.36 - i * .25));
                circle(cx, cy, radius * wave, rgba('0,0,0', 61 / 255 * (1 - wave) * opacity), 3);
            }
        else {
            const pulse = Math.abs(Math.sin(p * Math.PI * 4));
            circle(cx, cy, radius * (.54 + .2 * pulse), rgba('0,0,0', (.045 + .035 * pulse) * opacity));
        }
    }
    else if (kind === 'aurora') {
        const glow = easeOutQuint(clamp(p / .27)) * opacity, drift = easeOutQuint(p) * w * .22;
        c.fillStyle = rgba('7,13,32', 97 / 255 * glow);
        c.fillRect(0, 0, w, h);
        const xs = [w * .22 + drift, w * .76 - drift * .6, cx + drift * .35], ys = [h * .42, h * .57, h * .25], radii = [unit * .78, unit * .83, unit * .56];
        for (let i = 0; i < 3; i++) {
            const g = c.createRadialGradient(xs[i], ys[i], 0, xs[i], ys[i], radii[i]);
            g.addColorStop(0, rgba(colors[i], 110 / 255 * glow));
            g.addColorStop(.5, rgba(colors[i], 36 / 255 * glow));
            g.addColorStop(1, rgba(colors[i], 0));
            c.fillStyle = g;
            c.beginPath();
            c.arc(xs[i], ys[i], radii[i], 0, Math.PI * 2);
            c.fill();
        }
        for (let i = 0; i < 22; i++) {
            const rise = easeOutQuint(clamp(p * 1.3 - i * .019));
            circle(w * ((i * .6180339 + .11) % 1), h * ((i * .381966 + .19) % 1) - rise * h * .12, 1.4 + i % 3, rgba(colors[i % 2 === 0 ? 0 : 2], (.25 + .35 * rise) * glow));
        }
    }
    else if (kind === 'orbit') {
        const strength = easeOutQuint(clamp(p / .24)) * opacity, r = unit * (.34 + .16 * easeOutQuint(clamp(p / .24))), stroke = Math.max(1.4, unit * .002);
        circle(cx, cy, r * 1.22, rgba(colors[0], .09 * strength));
        for (let i = 0; i < 3; i++) {
            const ring = r * (1 + i * .18);
            circle(cx, cy, ring, rgba(colors[i], (.54 - i * .1) * strength), stroke);
            const a = (-105 + easeOutQuint(p) * (185 + i * 76) + i * 115) * Math.PI / 180;
            c.strokeStyle = rgba(colors[(i + 2) % 3], .86 * strength);
            c.lineWidth = stroke * (2.6 - i * .35);
            c.beginPath();
            c.arc(cx, cy, ring, a, a + (42 + i * 12) * Math.PI / 180);
            c.stroke();
        }
        for (let i = 0; i < 12; i++) {
            const a = (i * 30 + 105 + easeOutQuint(p) * 100) * Math.PI / 180, r2 = r * (1 + i % 3 * .18);
            circle(cx + Math.cos(a) * r2, cy + Math.sin(a) * r2, 2.5 + i % 3, rgba(colors[i % 2], strength));
        }
    }
    else if (kind === 'comet') {
        const strength = easeOutQuint(clamp(p / .2)) * opacity, travel = smoothStep(0, 1, p);
        for (let i = 0; i < 9; i++) {
            const x = -w * .25 + travel * (w + h) * (1.06 + i * .012) + i * w * .057, y = h * (i + .5) / 9 - travel * h * .42, tx = x - w * (.13 + i % 3 * .035), ty = y + h * .11;
            const g = c.createLinearGradient(tx, ty, x, y);
            g.addColorStop(0, rgba(colors[i % 2], 0));
            g.addColorStop(.7, rgba(colors[i % 2], .36 * strength));
            g.addColorStop(1, rgba('255,255,255', .85 * strength));
            c.strokeStyle = g;
            c.lineWidth = 2.1 + i % 3;
            c.beginPath();
            c.moveTo(tx, ty);
            c.lineTo(x, y);
            c.stroke();
            circle(x, y, 2 + i % 2, rgba('255,255,255', .8 * strength));
        }
        const x = cx - w * .6 + travel * w * 1.2, r = unit * .43, g = c.createRadialGradient(x, cy, 0, x, cy, r);
        g.addColorStop(0, rgba(colors[2], .18 * strength));
        g.addColorStop(1, rgba(colors[2], 0));
        c.fillStyle = g;
        c.beginPath();
        c.arc(x, cy, r, 0, Math.PI * 2);
        c.fill();
    }
    else
        circle(cx, cy, radius * easeOutQuint(clamp(p / .52)), rgba('244,196,48', opacity));
}
