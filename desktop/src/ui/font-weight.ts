import { availableWeights, nearestWeight } from '../core/fonts';
import { sliderRow } from './controls';
export function fontWeightControl(label: string, family: string, weight: number) {
    let weights = availableWeights(family);
    const names: Record<number, string> = { 100: 'Thin', 200: 'ExtraLight', 300: 'Light', 400: 'Regular', 500: 'Medium', 600: 'SemiBold', 700: 'Bold', 800: 'ExtraBold', 900: 'Black' };
    const row = sliderRow(label, 0, weights.length - 1, weights.indexOf(nearestWeight(family, weight)), index => String(weights[index]) + (names[weights[index]] ? ' · '+names[weights[index]] : ''));
    row.setEnabled(weights.length > 1);
    return { ...row, value: () => weights[+row.input.value], set(family: string, weight: number) { weights = availableWeights(family); row.setEnabled(weights.length > 1); row.input.max = String(weights.length - 1); row.input.value = String(weights.indexOf(nearestWeight(family, weight))); row.input.dispatchEvent(new Event('input')); } };
}
