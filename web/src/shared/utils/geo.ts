// Haversine 공식 기반 GPS 거리 계산 유틸

const EARTH_RADIUS_KM = 6371;

export function haversineKm(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos((lat1 * Math.PI) / 180) *
    Math.cos((lat2 * Math.PI) / 180) *
    Math.sin(dLon / 2) ** 2;
  return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

export function haversineM(lat1: number, lon1: number, lat2: number, lon2: number): number {
  return haversineKm(lat1, lon1, lat2, lon2) * 1000;
}

// 점 P에서 폴리라인(선분 배열)까지의 최단 거리 (m)
// 각 선분에 수직 투영 후 Haversine으로 최종 거리 계산
export function pointToPolylineM(
  lat: number,
  lon: number,
  polyline: { latitude: number; longitude: number }[]
): number {
  if (polyline.length === 0) return Infinity;
  if (polyline.length === 1) return haversineM(lat, lon, polyline[0].latitude, polyline[0].longitude);

  const DEG_TO_RAD = Math.PI / 180;
  const cosLat = Math.cos(lat * DEG_TO_RAD);

  let minDist = Infinity;

  for (let i = 0; i < polyline.length - 1; i++) {
    const ax = polyline[i].longitude * cosLat;
    const ay = polyline[i].latitude;
    const bx = polyline[i + 1].longitude * cosLat;
    const by = polyline[i + 1].latitude;
    const px = lon * cosLat;
    const py = lat;

    const abx = bx - ax;
    const aby = by - ay;
    const ab2 = abx * abx + aby * aby;

    // A=B인 경우 점 거리로 폴백
    if (ab2 === 0) {
      const d = haversineM(lat, lon, polyline[i].latitude, polyline[i].longitude);
      if (d < minDist) minDist = d;
      continue;
    }

    // 투영 계수 t ∈ [0, 1] 클램프
    const t = Math.max(0, Math.min(1, ((px - ax) * abx + (py - ay) * aby) / ab2));
    const closestLon = (ax + t * abx) / cosLat;
    const closestLat = ay + t * aby;

    const d = haversineM(lat, lon, closestLat, closestLon);
    if (d < minDist) minDist = d;
  }

  return minDist;
}
