import { prefs } from './prefs';
/** Browser capability values only: never fabricate battery level or signal strength. */
export class DisplaySettings {
    private readonly status = document.createElement('div');
    private timer: number | undefined;
    private battery?: EventTarget & {
        level: number;
        charging: boolean;
    };
    constructor() {
        this.status.className = 'device-status';
        this.status.setAttribute('role', 'status');
        document.getElementById('app')!.append(this.status);
        window.addEventListener('online', () => this.updateStatus());
        window.addEventListener('offline', () => this.updateStatus());
        document.addEventListener('visibilitychange', () => this.refresh());
        const nav = navigator as Navigator & {
            getBattery?: () => Promise<EventTarget & {
                level: number;
                charging: boolean;
            }>;
        };
        void nav.getBattery?.().then(battery => { this.battery = battery; for (const event of ['levelchange', 'chargingchange'])
            battery.addEventListener(event, () => this.updateStatus()); this.updateStatus(); }).catch(() => { });
        this.refresh();
    }
    refresh(): void {
        clearInterval(this.timer);
        const options = prefs.getUltimateOptions();
        const root = document.documentElement;
        root.style.setProperty('--safe-top', options.avoidCutout ? 'env(safe-area-inset-top,0px)' : '0px');
        root.style.setProperty('--safe-bottom', options.avoidCutout ? 'env(safe-area-inset-bottom,0px)' : '0px');
        root.style.setProperty('--display-safe-top', options.avoidCutout ? 'env(safe-area-inset-top,0px)' : '0px');
        root.style.setProperty('--display-safe-left', options.avoidCutout ? 'env(safe-area-inset-left,0px)' : '0px');
        root.style.setProperty('--display-safe-right', options.avoidCutout ? 'env(safe-area-inset-right,0px)' : '0px');
        root.style.setProperty('--protection-brightness', options.burnIn && options.burnDim ? '.82' : '1');
        root.style.setProperty('--burn-margin', options.burnIn ? options.burnAmplitude + 'px' : '0px');
        this.shift();
        this.updateStatus();
        if (options.burnIn && !document.hidden)
            this.timer = window.setInterval(() => this.shift(), options.burnInterval * 60000);
    }
    private shift(): void {
        const options = prefs.getUltimateOptions(), root = document.documentElement;
        const positions = [[0, 0], [1, 0], [1, 1], [0, 1], [-1, 1], [-1, 0], [-1, -1], [0, -1], [1, -1]];
        const position = positions[Math.floor(Date.now() / (options.burnInterval * 60000)) % positions.length];
        root.style.setProperty('--burn-x', (options.burnIn ? position[0] * options.burnAmplitude : 0) + 'px');
        root.style.setProperty('--burn-y', (options.burnIn ? position[1] * options.burnAmplitude : 0) + 'px');
    }
    private updateStatus(): void {
        const options = prefs.getUltimateOptions();
        this.status.hidden = !options.statusIcons;
        this.status.dataset.style = options.statusStyle;
        this.status.style.fontSize = 12 * options.statusScale / 100 + 'px';
        const en = prefs.isClockUseEnglish();
        const network = navigator.onLine ? (en ? 'Online' : '已连接') : (en ? 'Offline' : '离线');
        const battery = this.battery ? ' · ' + (this.battery.charging ? '⚡ ' : '') + Math.round(this.battery.level * 100) + '%' : '';
        this.status.textContent = network + battery;
        this.status.title = this.battery ? '' : (en ? 'Battery information unavailable in this browser' : '此浏览器未提供电池信息');
    }
}
