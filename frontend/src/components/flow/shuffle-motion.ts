// Nose, shoulders, elbows, wrists, hips, knees and ankles in the shared pose order.
const ready = [0,-62, 23,-27, -23,-27, 40,14, -40,14, 30,44, -30,44, 18,73, -18,73, 25,147, -25,147, 29,225, -29,225];
const slide = [20,-66, 38,-29, -8,-34, 78,-43, -27,9, 102,-15, 4,16, 25,69, -11,69, 63,141, -13,146, 91,225, -14,225];
const tap = [37,-78, 55,-41, 9,-39, 87,-8, -12,-12, 68,17, 15,-35, 38,61, 3,61, 55,134, 3,123, 68,225, 34,191];
const clap = [0,-58, 23,-22, -23,-22, 54,10, -54,10, 4,1, -4,1, 18,78, -18,78, 32,145, -32,145, 24,225, -24,225];

function mirror(pose: number[]) {
  const pairOrder = [0, 2, 1, 4, 3, 6, 5, 8, 7, 10, 9, 12, 11];
  return pairOrder.flatMap(index => [-pose[index * 2], pose[index * 2 + 1]]);
}

const beats = [ready, slide, tap, clap, ready, mirror(slide), mirror(tap), clap];
const framesPerBeat = 14;

function makeTrack(origin: number, mirrored: boolean) {
  const poses = mirrored ? beats.map(mirror) : beats;
  return poses.flatMap((pose, index) => {
    const next = poses[(index + 1) % poses.length];
    return Array.from({ length: framesPerBeat }, (_, frame) => {
      const t = frame / framesPerBeat;
      const progress = t * t * (3 - 2 * t);
      return pose.map((value, joint) => value + (next[joint] - value) * progress + (joint % 2 === 0 ? origin : 0));
    });
  });
}

export const shuffleMotion = [makeTrack(166, false), makeTrack(404, true)];
