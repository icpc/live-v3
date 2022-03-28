// in posts.js
import * as React from "react";
import { List, Datagrid, Edit, Create, SimpleForm, TextField, EditButton, TextInput, ShowButton, ImageField } from "react-admin";

const postFilters = [
    <TextInput key="search" label="Search" source="q" alwaysOn />,
];

export const AdvertisementList = (props) => (
    <List title="Advertisement" filters={postFilters} {...props}>
        <Datagrid>
            <TextField source="text" />
            <ShowButton basePath="/showAdvertisement" label="show" onClick={() => {
                console.log("Show");
                // http://172.18.1.214:8080/adminapi/advertisement;
            }} />
            <EditButton basePath="/advertisement" />
        </Datagrid>
    </List>
);

const AdvertisementTitle = ({ record }) => {
    return <span>Post {record ? `"${record.title}"` : ""}</span>;
};

export const AdvertisementEdit = (props) => (
    <Edit title={<AdvertisementTitle />} {...props}>
        <SimpleForm>
            <TextInput source="text" />
        </SimpleForm>
    </Edit>
);

export const AdvertisementCreate = (props) => (
    <Create title="Create an advertisement" {...props}>
        <SimpleForm>
            <TextInput source="text" />
        </SimpleForm>
    </Create>
);

export const PictureList = (props) => (
    <List title="Picture" filters={postFilters} {...props}>
        <Datagrid>
            <ImageField source="picture" />
            <TextField source="text" />
            <ShowButton basePath="/showPicture" label="show" onClick={() => {
                console.log("Show");
                // http://172.18.1.214:8080/adminapi/advertisement;
            }} />
            <EditButton basePath="/picture" />
        </Datagrid>
    </List>
);

const PictureTitle = ({ record }) => {
    return <span>Post {record ? `"${record.title}"` : ""}</span>;
};

export const PictureEdit = (props) => (
    <Edit title={<PictureTitle />} {...props}>
        <SimpleForm>
            <TextInput source="text" />
        </SimpleForm>
    </Edit>
);

export const PictureCreate = (props) => (
    <Create title="Create a picture" {...props}>
        <SimpleForm>
            <TextInput source="text" />
        </SimpleForm>
    </Create>
);
