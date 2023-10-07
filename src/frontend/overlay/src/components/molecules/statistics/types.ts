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

export interface CriterionDescription {
    readonly criterion: string;
    readonly caption: string;
    readonly color: string;
}

export interface StackedBarsData {
    readonly criterions: CriterionDescription[];
    readonly bars: BarData[];
}
