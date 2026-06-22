// SVG placeholder avatars as data URIs for different roles
export const teacherPlaceholder = 'data:image/svg+xml;base64,' + btoa(
  `<svg xmlns="http://www.w3.org/2000/svg" width="200" height="200" viewBox="0 0 200 200">
    <rect width="200" height="200" fill="#2d3f66"/>
    <circle cx="100" cy="80" r="40" fill="#444f66"/>
    <path d="M40 170c0-33 27-60 60-60s60 27 60 60" fill="#444f66"/>
    <rect x="65" y="130" width="70" height="8" rx="4" fill="#5a6a8a"/>
  </svg>`
)

export const studentPlaceholder = 'data:image/svg+xml;base64,' + btoa(
  `<svg xmlns="http://www.w3.org/2000/svg" width="200" height="200" viewBox="0 0 200 200">
    <rect width="200" height="200" fill="#2d5a7a"/>
    <circle cx="100" cy="80" r="40" fill="#3d7a9a"/>
    <path d="M40 170c0-33 27-60 60-60s60 27 60 60" fill="#3d7a9a"/>
    <rect x="65" y="130" width="70" height="8" rx="4" fill="#5a9aba"/>
  </svg>`
)

export const adminPlaceholder = 'data:image/svg+xml;base64,' + btoa(
  `<svg xmlns="http://www.w3.org/2000/svg" width="200" height="200" viewBox="0 0 200 200">
    <rect width="200" height="200" fill="#4a3728"/>
    <circle cx="100" cy="80" r="40" fill="#6a5748"/>
    <path d="M40 170c0-33 27-60 60-60s60 27 60 60" fill="#6a5748"/>
    <rect x="65" y="130" width="70" height="8" rx="4" fill="#8a7768"/>
  </svg>`
)