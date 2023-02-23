import { pushLog } from "../../redux/debug";
import { addMessage, removeMessage, setMessages } from "../../redux/ticker";

export const handleMessage = (dispatch, e) => {
    const message = JSON.parse(e.data);
    switch (message.type) {
    case "AddMessage":
        dispatch(addMessage(message.message));
        break;
    case "RemoveMessage":
        dispatch(removeMessage(message.messageId));
        break;
    case "TickerSnapshot":
        dispatch(setMessages(message.messages));
        break;
    default:
        dispatch(pushLog(`UNKNOWN MESSAGE TYPE: ${message.type}`));
        break;
    }
};
