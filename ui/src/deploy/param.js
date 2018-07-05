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
            this.addValueToParam(param);
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

    renderSingleField(param) {
        return <Field
            index={0}
            param={param}
            onChange={this.change}
            onAdd={this.add}
            bare={false}
        />;
    }

    renderMultipleFields(param) {
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

    render() {
        let param = this.props.param;
        let parent;
        let children;

        if (Array.isArray(param.params)) {
            parent = this.renderSingleField(param);
            children = this.renderChildren(param);
        } else {
            parent = this.renderMultipleFields(param)
        }

        return (
            <div>
                {parent}
                {children}
            </div>
        );
    }
}

Field.propTypes = {
    param: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
    onAdd: PropTypes.func.isRequired
};