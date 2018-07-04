import React from "react";
import Param from "../param";
import PropTypes from 'prop-types';

export default class Form extends React.Component {

    constructor(props) {
        super(props);
        this.state = {};
        this.changeParam = this.changeParam.bind(this);
        this.addParam = this.addParam.bind(this);
    }

    changeParam(newFormParam) {
        let form = this.props.form;
        let index = this.findParamIndex(newFormParam.id, form.params);
        form.params[index] = newFormParam;
        this.props.onChange(form);
    }

    addParam(param) {
        let copy = Object.assign({}, param);
        let params = this.findContainingParams(param, this.props.form);
        let index = this.findParamIndex(param.id, params);
        params.splice(index, 0, copy);
        copy.id = params.length;
        if(Array.isArray(copy.params)) {
            copy.params.forEach((param) => {param.parentId = copy.id});
        }
        delete copy.value;
        this.props.onChange(this.props.form);
    }

    findContainingParams(param, form) {
        if (isNaN(param.parentId)) {
            return form.params;
        }
        let parentIndex = this.findParamIndex(param.parentId, form.params);
        return form.params[parentIndex].params;
    }

    findParamIndex(id, params) {
        return params.findIndex(
            (p) => Number(p.id) === Number(id)
        );
    }

    render() {
        const form = this.props.form;

        return (
            <form>
                {form.params.map((param, i) => {
                    return <Param
                        key={i}
                        param={param}
                        onChange={this.changeParam}
                        onAdd={this.addParam}
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