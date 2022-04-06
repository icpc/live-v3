import React from "react";

export const ErrorContext = React.createContext({
    error: undefined,
});

export const addErrorHandler = (cause) => {
    // const [_, setContext] = useContext(ErrorContext);

    return (error) => {
        console.log(cause + ":", error);
        //todo: show alert

        // ErrorContext.data = ({ error: cause });
    };
};


