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

    remove = (index) => (param) => {
        if (Array.isArray(param.params)) {
            this.props.onRemove();
        } else {
            this.removeValueFromParam(param, index);
            this.change(param);
        }
    };

    removeChild = (index) => (valueIndex) => {
        let param = this.props.param;
        this.removeValueFromParam(param.params[index], valueIndex);
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
     * NB. Does not handle a param with child params
     * (is delegated to this.props.onAdd).
     */
    setAddableAndRemovable(param) {
        let min = Number(param.minimumCardinality);
        param.canRemove = min === 0 || min < param.value.length;
        let max = param.maximumCardinality;
        param.canAdd = max === '*' || Number(max) > param.value.length;
    }

    renderSingleValue(param) {
        return <Field
            index={0}
            param={param}
            onChange={this.change}
            onAdd={this.add}
            onRemove={this.remove()}
            bare={false}
        />;
    }

    renderMultipleValues(param) {
        return param.value.map((value, i) => {
            return <Field
                key={i}
                index={i}
                param={param}
                onChange={this.change}
                onAdd={this.add}
                onRemove={this.remove(i)}
                bare={i > 0}
            />
        }, this);
    }

    renderChildParams(param) {
        return param.params.map((childParam, i) => {
            return childParam.value.map((value, k) => {
                return <Field
                    key={k}
                    index={k}
                    param={childParam}
                    onChange={this.changeChild(i)}
                    onAdd={this.addChild(i)}
                    onRemove={this.removeChild(i, k)}
                    bare={k > 0}
                />
            }, this)
        }, this);
    }

    render() {
        let param = this.props.param;
        let hasChildren = Array.isArray(param.params);
        let parent;
        let children;

        if (hasChildren) {
            parent = this.renderSingleValue(param);
            children = this.renderChildParams(param);
        } else {
            parent = this.renderMultipleValues(param)
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