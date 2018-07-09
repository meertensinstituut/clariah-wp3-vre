import React from "react";
import String from "./string";
import Select from "./select";
import Integer from "./integer";
import File from "./file";
import PropTypes from 'prop-types';
import './field.css';
import AddButton from "./add-button";

const PARAM_TO_CLASS = new Map();
PARAM_TO_CLASS.set("string", String);
PARAM_TO_CLASS.set("integer", Integer);
PARAM_TO_CLASS.set("enumeration", Select);
PARAM_TO_CLASS.set("file", File);

export default class Field extends React.Component {

    constructor(props) {
        super(props);
        this.state = {};
    }

    handleChange = () => (value) => {
        this.props.param.value[this.props.index] = value;
        this.props.onChange(this.props.param);
    };

    handleAdd = () => () => {
        this.props.onAdd(this.props.param);
    };

    handleRemove = () => () => {
        this.props.onRemove(this.props.param);
    };

    hasAddButton(field) {
        return this.props.bare === false
            && field !== null;
    }

    static getFieldTypeByParam(type) {
        for (let [paramType, classType] of PARAM_TO_CLASS) {
            if (type === paramType) {
                return classType;
            }
        }
        return null;
    }

    renderFieldByParamType() {
        let param = this.props.param;
        let classType = Field.getFieldTypeByParam(param.type);
        if (classType === null) {
            return null;
        }
        let value = param.value[this.props.index];
        return React.createElement(
            classType, {
                param: param,
                value: value,
                onChange: this.handleChange(),
                canRemove: this.canRemoveField(),
                onRemove: this.handleRemove,
            }
        );
    }

    canRemoveField() {
        return this.props.param.canRemove && !Array.isArray(this.props.param.params);
    }

    renderAddButton(field) {
        if (!this.hasAddButton(field)) return null;
        return <AddButton
            canAdd={this.props.param.canAdd}
            onAdd={this.handleAdd}
        />;
    }

    render() {
        let field = this.renderFieldByParamType();
        return (
            <div className="param-field">
                {this.renderAddButton(field)}
                <span>
                    <label>{this.props.bare ? null : this.props.param.label}</label>
                    <p>{this.props.bare ? null : this.props.param.description}</p>
                </span>
                {field}
            </div>
        );
    }
}

Field.propTypes = {
    param: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
    onAdd: PropTypes.func.isRequired,
    onRemove: PropTypes.func.isRequired
};

Field.defaultProps = {
    canAdd: true,
    bare: false
};