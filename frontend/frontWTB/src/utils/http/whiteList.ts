export const NO_TOKEN_URLS = [
    "app/login/getCode",
    "app/login",
  "/user/code",
  "/user/login",
  "/blog/hot",
  "/shop",
  "/shop-type",
  "/shopping/show",
  "/upload",
  "/voucher"
];

export function isNoTokenUrl(url?: string) {
  if (!url) return false;
  return NO_TOKEN_URLS.some(prefix =>
    url === prefix || url.startsWith(prefix + "/")
  );
}
