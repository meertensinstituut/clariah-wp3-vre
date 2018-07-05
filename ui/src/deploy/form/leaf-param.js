import React from "react";
import Field from "./field";

/**
 * Param without child params.
 * Added fields are stored as elements in value[]
 */
export default class LeafParam extends React.Component {

    change = (param) => {
        this.props.onChange(param);
    };

    remove = (index) => (param) => {
        this.removeValueFromParam(param, index);
        this.change(param);
    };

    add = (param) => {
        this.addValueToParam(param);
        this.change(param);
    };

    addValueToParam(param) {
        if (!param.canAdd) {
            return;
        }
        param.value.push("");
        this.setAddableAndRemovable(param);
    }

    removeValueFromParam(param, valueIndex) {
        if (!param.canRemove) {
            return;
        }
        param.value.splice(valueIndex, 1);
        this.setAddableAndRemovable(param);
    }

    /**
     * Setting of canAdd and canRemove is based on
     * cardinality and number of elements in value[].
     */
    setAddableAndRemovable(param) {
        let min = Number(param.minimumCardinality);
        param.canRemove = min === 0 || min < param.value.length;
        let max = param.maximumCardinality;
        param.canAdd = max === '*' || Number(max) > param.value.length;
    }

    render() {
        let param = this.props.param;
        return (
            <div>
                {param.value.map((value, i) => {
                    return <Field
                        key={i}
                        index={i}
                        param={param}
                        onChange={this.change}
                        onAdd={this.add}
                        onRemove={this.remove(i)}
                        bare={i > 0}
                    />
                }, this)}
            </div>
        );
    }
}