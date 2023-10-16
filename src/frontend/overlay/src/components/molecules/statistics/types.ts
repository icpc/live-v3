export interface BarValue {
    readonly color: string;
    readonly caption: string;
    readonly value: number;
}

export interface BarData {
    readonly name: string;
    readonly color: string;
    readonly values: BarValue[];
}

export interface LegendDescription {
    readonly caption: string;
    readonly color: string;
}

export type Legend = LegendDescription[];

export type StackedBarsData = BarData[];

export interface StatisticsData {
    readonly data: StackedBarsData; // | other statistics data
    readonly legend: Legend;
}
