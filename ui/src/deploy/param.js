import React from "react";
import Field from "./form/field";
import PropTypes from 'prop-types';

export default class Param extends React.Component {

    constructor(props) {
        super(props);
        this.state = {};
    }

    change = (param) => {
        this.props.onChange(param);
    };

    changeChild = (index) => (child) => {
        let param = this.props.param;
        param.params[index] = child;
        this.change(param);
    };

    add = (param) => {
        if (Array.isArray(param.params)) {
            this.props.onAdd();
        } else {
            this.props.param.value.push("");
            this.change(param);
        }
    };

    addChild = (index) => () => {
        let param = this.props.param;
        this.addValueToParam(param.params[index]);
        this.change(param);
    };

    addValueToParam(param) {
        if (!this.addable(param)) {
            return;
        }
        param.value.push("");
    }

    addable(param) {
        if (param.maximumCardinality === '*') return true;
        return Number(param.maximumCardinality) > param.value.length;
    }

    render() {
        let param = this.props.param;
        let children = Array.isArray(param.params)
            ?
            this.renderChildren(param)
            :
            this.renderParamValues(param);

        let withParams = Array.isArray(param.params)
            ?
            <Field
                param={param}
                onChange={this.change}
                onAdd={this.add}
                bare={false}
            />
            :
            null;

        return (
            <div>
                {withParams}
                {children}
            </div>
        );
    }

    renderParamValues(param) {
        return param.value.map((value, i) => {
            return <Field
                key={i}
                index={i}
                param={param}
                onChange={this.change}
                onAdd={this.add}
                bare={i > 0}
            />
        }, this);
    }

    renderChildren(param) {
        return param.params.map((childParam, i) => {
            return childParam.value.map((value, k) => {
                return <Field
                    key={k}
                    index={k}
                    param={childParam}
                    onChange={this.changeChild(i)}
                    onAdd={this.addChild(i)}
                    bare={k > 0}
                />
            }, this)
        }, this);
    }
}

Field.propTypes = {
    param: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
    onAdd: PropTypes.func.isRequired
};