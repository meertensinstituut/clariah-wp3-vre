import React from "react";
import Param from "../param";
import PropTypes from 'prop-types';

export default class Form extends React.Component {

    constructor(props) {
        super(props);
        this.state = {};
        this.changeParam = this.changeParam.bind(this);
    }

    changeParam(newFormParam) {
        let form = this.props.form;
        let params = this.findContainingParams(newFormParam, form);
        let index = this.findParamIndex(newFormParam.id, form.params);
        params[index] = newFormParam;
        this.props.onChange(form);
    }

    addParam = (index) => () => {
        let form = this.props.form;
        let copy = Object.assign({}, form.params[index]);
        console.log("old, new:", form.params[index], copy);
        copy.id = form.params.length;
        copy.params.forEach((p) => p.parentId = copy.id);
        form.params.splice(index + 1, 0, copy);
        this.props.onChange(form);
    };

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
                        onAdd={this.addParam(i)}
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