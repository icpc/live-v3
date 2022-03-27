// in posts.js
import * as React from "react";
import { List, Datagrid, Edit, Create, SimpleForm, TextField, EditButton, TextInput, ShowButton } from 'react-admin';

export const AdvertisementList = (props) => (
    <List {...props}>
        <Datagrid>
            <TextField source="title" />
            <TextField source="subtitle" />
            <ShowButton basePath="/showAdvertisement" label="show"/>
            <EditButton basePath="/advertisement" />
        </Datagrid>
    </List>
);

const AdvertisementTitle = ({ record }) => {
    return <span>Post {record ? `"${record.title}"` : ''}</span>;
};

export const AdvertisementEdit = (props) => (
    <Edit title={<AdvertisementTitle />} {...props}>
        <SimpleForm>
            <TextInput source="title" />
            <TextInput source="subtitle" />
        </SimpleForm>
    </Edit>
);

export const AdvertisementCreate = (props) => (
    <Create title="Create a advertisement" {...props}>
        <SimpleForm>
            <TextInput source="title" />
            <TextInput source="subtitle" />
        </SimpleForm>
    </Create>
);
