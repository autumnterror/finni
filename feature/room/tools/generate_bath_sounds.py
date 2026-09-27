"""Generate the small, original bathroom effects used by SoundPool."""

from pathlib import Path
import wave

import numpy as np


RATE = 22_050
OUTPUT = Path(__file__).resolve().parents[1] / "src/main/assets/audio"
RNG = np.random.default_rng(20260928)


def smoothed_noise(count: int, width: int) -> np.ndarray:
    noise = RNG.standard_normal(count)
    return np.convolve(noise, np.ones(width) / width, mode="same")


def save(name: str, samples: np.ndarray) -> None:
    fade_count = int(RATE * 0.025)
    envelope = np.ones(len(samples))
    envelope[:fade_count] = np.linspace(0.0, 1.0, fade_count)
    envelope[-fade_count:] = np.linspace(1.0, 0.0, fade_count)
    samples *= envelope
    samples *= 0.82 / max(1e-6, np.max(np.abs(samples)))
    OUTPUT.mkdir(parents=True, exist_ok=True)
    with wave.open(str(OUTPUT / name), "wb") as output:
        output.setnchannels(1)
        output.setsampwidth(2)
        output.setframerate(RATE)
        output.writeframes((samples * 32767).astype("<i2").tobytes())


def soap_rubbing() -> np.ndarray:
    seconds = 0.72
    t = np.arange(int(RATE * seconds)) / RATE
    rub = (0.5 + 0.5 * np.sin(2 * np.pi * 3.2 * t - 0.5)) ** 2
    swish = smoothed_noise(len(t), 9) - 0.3 * smoothed_noise(len(t), 45)
    frequency = 510 + 155 * np.sin(2 * np.pi * 3.2 * t)
    squeak = np.sin(2 * np.pi * np.cumsum(frequency) / RATE)
    return 0.48 * swish * (0.25 + 0.9 * rub) + 0.08 * squeak * rub


def shower_water() -> np.ndarray:
    seconds = 1.20
    t = np.arange(int(RATE * seconds)) / RATE
    fine = smoothed_noise(len(t), 3)
    splash = smoothed_noise(len(t), 13)
    flow = (0.32 * fine + 0.57 * splash) * (
        0.9 + 0.07 * np.sin(2 * np.pi * 6 * t)
    )
    for start_seconds in np.arange(0.08, seconds - 0.03, 0.095):
        start = int(start_seconds * RATE)
        length = min(int(RATE * 0.035), len(flow) - start)
        local = np.arange(length) / RATE
        decay = np.exp(-local * 115)
        flow[start : start + length] += (
            0.13 * np.sin(2 * np.pi * (280 * local + 55 * local**2)) * decay
        )
    return flow


if __name__ == "__main__":
    save("bathroom_soap_rubbing.wav", soap_rubbing())
    save("bathroom_shower_water.wav", shower_water())
