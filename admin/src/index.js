import React from "react";
import ReactDOM from "react-dom";
import { Admin, fetchUtils, Resource } from "react-admin";
import simpleRestProvider from "ra-data-simple-rest";
import { AdvertisementList, AdvertisementCreate, AdvertisementEdit } from "./posts";
import { PictureList, PictureCreate, PictureEdit } from "./posts";
import { Layout, AppBar, UserMenu, Menu, MenuItemLink } from "react-admin";
// import fakeDataProvider from "ra-data-fakerest";
import routs from "./routs";

// const fetchJson = (url, options = {}) => {
//     console.log(options);
//     console.log(url);
//     if (!options.headers) {
//         options.headers = new Headers({ Accept: "application/json" });
//     }
//     options.headers.set("Access-Control-Expose-Headers", "X-Total-Count");
//     return fetchUtils.fetchJson(url, options);
// };

export const dataProvider = simpleRestProvider("http://localhost:8080/adminapi", fetchUtils.fetchJson, "X-Total-Count");
// const dataProvider = fakeDataProvider({
//     advertisement: [
//         { text: "Контест начался" },
//         { text: "Николай Будуин" },
//     ],
//     picture: [
//         { picture: "https://upload.wikimedia.org/wikipedia/en/7/7d/Lenna_%28test_image%29.png", text: "Контест начался" },
//         { text: "Николай Будуин" },
//     ],
//     comments: [
//         { id: 0, post_id: 0, author: "John Doe", body: "Sensational!" },
//         { id: 1, post_id: 0, author: "Jane Doe", body: "I agree" },
//     ],
//     queue: [
//         { state: false }
//     ],
//     standings: [
//         { state: false }
//     ]
// });

const LiveMenu = (props) => (
    <Menu {...props}>
        <MenuItemLink to="/controls" primaryText="Controls" />
        <MenuItemLink to="/advertisement" primaryText="Advertisement" />
        <MenuItemLink to="/picture" primaryText="Picture" />
    </Menu>
);

const ConfigurationMenu = React.forwardRef(({ onClick }, ref) => (
    <MenuItemLink
        ref={ref}
        to="/configuration"
        primaryText="Configuration"
        // onClick={onClick} // close the menu on click
    />
));

ConfigurationMenu.displayName="ConfigurationMenu";

const LiveUserMenu = props => (
    <UserMenu {...props}>
        <ConfigurationMenu />
    </UserMenu>
);

const LiveAppBar = props => <AppBar {...props} userMenu={<LiveUserMenu />} />;
const LiveLayout = (props) => <Layout {...props} appBar={LiveAppBar} menu={LiveMenu} />;

ReactDOM.render(
    <Admin layout={LiveLayout} customRoutes={routs} dataProvider={dataProvider}>
        {/* <BooleanInput name="queue"/> */}
        {/* <MyComponent name='kek'/> */}
        <Resource name="advertisement" list={AdvertisementList} edit={AdvertisementEdit} create={AdvertisementCreate} />
        <Resource name="picture" list={PictureList} edit={PictureEdit} create={PictureCreate} />
        {/* </> */}
    </Admin>,
    document.getElementById("root")
);
