import { useEffect, useState } from "react";

export const useLocation = () => {
    const [location, setLocation] = useState(window.location);
    useEffect(() => {
        const onLocationChange = () => setLocation(window.location);
        window.addEventListener("popstate", onLocationChange);
        return () => window.removeEventListener("popstate", onLocationChange);
    }, []);
    return location;
};

export const useQueryParams = () => {
    const { search } = useLocation();
    return new URLSearchParams(search);
};
