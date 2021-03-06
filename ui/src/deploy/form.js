import React from "react";
import BranchParam from "./branch-param";
import PropTypes from 'prop-types';
import Field from "./fields/field";

/**
 * Form generated from Params
 */
export default class Form extends React.Component {

    constructor(props) {
        super(props);
        let params = this.props.form.params;
        params.forEach((p) => {
            this.setAddAndRemove(p, params);
            if (Array.isArray(p.params)) {
                p.params.forEach((cp) => this.setAddAndRemove(cp, p.params))
            }
        });
        this.setValid(this.props.form);
    }

    setValid(form) {
        let valid = true;
        form.params.forEach((p) => {
            valid = this.validateParam(p);
        });
        form.valid = valid;

    }

    validateParam(p) {
        let valid = true;
        p.value.forEach((v) => {
            let fieldClass = Field.getFieldTypeByParam(p.type);
            let validation = Array.isArray(p.values)
                ? fieldClass.handleValidation(v, p.values.map(v => v.value))
                : fieldClass.handleValidation(v);
            if (validation !== 'success') {
                valid = false;
            }
        });
        if (Array.isArray(p.params)) {
            p.params.forEach(cp => valid = this.validateParam(cp, cp.value));
        }
        return valid;
    }

    addParam = (indexToCopy) => () => {
        let form = this.props.form;
        let copy = this.createCopy(form.params[indexToCopy]);
        form.params.splice(indexToCopy + 1, 0, copy);
        this.setAllAddAndRemove(form.params, copy.name);
        this.handleChange(form);
    };

    createCopy(param) {
        let copy = JSON.parse(JSON.stringify(param));
        copy.value = [""];
        copy.params.forEach((p) => {
            p.value = [""];
            this.setAddAndRemove(p, copy.params);
        });
        return copy;
    }

    changeParam = (index) => (newFormParam) => {
        let form = this.props.form;
        form.params[index] = newFormParam;
        this.handleChange(form);
    };

    removeParam = (indexToRemove) => () => {
        let form = this.props.form;
        let params = form.params;
        let nameRemoved = params[indexToRemove].name;
        params.splice(indexToRemove, 1);
        this.setAllAddAndRemove(params, nameRemoved);
        this.handleChange(form);
    };

    handleChange(form) {
        this.setValid(form);
        this.props.onChange(form);
    }

    setAllAddAndRemove(params, withFieldName) {
        params.forEach((p) => {
            if (p.name === withFieldName) {
                this.setAddAndRemove(p, params);
            }
        });
    }

    setAddAndRemove(param, siblings) {
        param.canAdd = this.canAddParam(param, siblings);
        param.canRemove = this.canRemoveParam(param, siblings);
    }

    canRemoveParam(param, siblings) {
        let min = Number(param.minimumCardinality);
        if (min === 0) {
            return true;
        }
        let hasChildParams = Array.isArray(param.params);
        if (!hasChildParams) {
            return param.value.length > min;
        }
        let copies = siblings.filter((p) => p.name === param.name).length;
        if (hasChildParams) {
            return min < copies;
        }
    }

    canAddParam(param, siblings) {
        if (param.maximumCardinality === '*') {
            return true;
        }
        let hasChildParams = Array.isArray(param.params);
        let max = Number(param.maximumCardinality);
        if (!hasChildParams) {
            return param.value.length < max;
        }
        let copies = siblings.filter((p) => p.name === param.name).length;
        if (hasChildParams) {
            return max > copies;
        }
    }

    render() {
        const form = this.props.form;

        return (
            <form>
                {form.params.map((param, i) => {
                    return <BranchParam
                        key={i}
                        param={param}
                        onAdd={this.addParam(i)}
                        onChange={this.changeParam(i)}
                        onRemove={this.removeParam(i)}
                    />;
                }, this)}
            </form>
        );
    }

}

Form.propTypes = {
    form: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired
};