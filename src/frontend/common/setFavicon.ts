export const setFavicon = (svg: string) => {
    const link = document.createElement("link");
    link.type = "image/x-icon";
    link.rel = "shortcut icon";
    link.href = "data:image/svg+xml;base64," + btoa(svg);
    document.head.appendChild(link);
};
