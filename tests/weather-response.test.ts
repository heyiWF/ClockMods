import { expect, it } from 'vitest';
import { readWeatherJson } from '../src/weather/qweather';

it('parses a bounded UTF-8 weather response', async () => {
  expect(await readWeatherJson(new Response(' {"text":"晴"} '))).toEqual({ text: '晴' });
});
it('rejects oversized chunked data without relying on Content-Length', async () => {
  const stream = new ReadableStream<Uint8Array>({ start(controller) { controller.enqueue(new Uint8Array(1024 * 1024 + 1)); } });
  await expect(readWeatherJson(new Response(stream))).rejects.toThrow('response too large');
});
it('rejects an oversized declared body before consuming it', async () => {
  await expect(readWeatherJson(new Response('{}', { headers: { 'content-length': '1048577' } }))).rejects.toThrow('response too large');
});
