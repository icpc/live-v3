import React from "react";
import { BooleanInput, SimpleForm, Title } from "react-admin";

export const Controls = (...props) => (
    <SimpleForm>
        <Title title="Controls" />
        <BooleanInput name='queue' label='Queue' />
        <BooleanInput name='scoreboard' label='Scoreboard' />
        <BooleanInput name='statistic' label='Statistic' />
        <BooleanInput name='ticker' label='Ticker' />
    </SimpleForm>
);

