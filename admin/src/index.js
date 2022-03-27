import React from 'react';
import ReactDOM from 'react-dom';
import { Admin, Resource } from 'react-admin';
import { AdvertisementList, AdvertisementCreate, AdvertisementEdit } from './posts';
import { Layout, AppBar, UserMenu, Menu, MenuItemLink } from 'react-admin';
import fakeDataProvider from 'ra-data-fakerest';


const dataProvider = fakeDataProvider({
  advertisement: [
      { title: 'Контест начался', subtitle: 'У школьников есть пять часов на решение предложенных жюри задач' },
      { title: 'Николай Будуин', subtitle: 'Молодец' },
  ],
  comments: [
      { id: 0, post_id: 0, author: 'John Doe', body: 'Sensational!' },
      { id: 1, post_id: 0, author: 'Jane Doe', body: 'I agree' },
  ],
});

const MyMenu = (props) => (
  <Menu {...props}>
      <MenuItemLink to="/posts" primaryText="Posts"/>
      <MenuItemLink to="/comments" primaryText="Comments"/>
      <MenuItemLink to="/users" primaryText="Users"/>
      <MenuItemLink to="/custom-route" primaryText="Miscellaneous"/>
  </Menu>
);
const ConfigurationMenu = React.forwardRef(({ onClick }, ref) => (
  <MenuItemLink
      ref={ref}
      to="/configuration"
      primaryText="Configuration"
      onClick={onClick} // close the menu on click
  />
));
const MyUserMenu = props => (
  <UserMenu {...props}>
      <ConfigurationMenu />
  </UserMenu>
);
const MyAppBar = props => <AppBar {...props} userMenu={<MyUserMenu />} />;
const MyLayout = (props) => <Layout {...props} appBar={MyAppBar} />;


ReactDOM.render(
  <Admin layout={MyLayout} dataProvider={dataProvider}>
      <Resource name="advertisement" list={AdvertisementList} edit={AdvertisementEdit} create={AdvertisementCreate} />
  </Admin>,
  document.getElementById('root')
);
