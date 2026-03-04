const BASE_URL = import.meta.env.VITE_MINIO_BASE_URL

/**
 * 将资源路径转为完整可访问 URL
 */
export function resolveAssetUrl(path?: string) {
  if (!path) return ''
  if (path.startsWith('http')) return path
  return BASE_URL + path
}
