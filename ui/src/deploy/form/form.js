import React from "react";
import Param from "../param";
import PropTypes from 'prop-types';

export default class Form extends React.Component {

    constructor(props) {
        super(props);
        this.state = {};
        this.changeParam = this.changeParam.bind(this);
    }

    changeParam = (index) => (newFormParam) => {
        let form = this.props.form;
        form.params[index] = newFormParam;
        this.props.onChange(form);
    };

    addParam = (index) => () => {
        let form = this.props.form;
        let copy = JSON.parse(JSON.stringify(form.params[index]));
        copy.params.forEach((p) => {
            p.value = [""];
        });
        form.params.splice(index + 1, 0, copy);
        this.props.onChange(form);
    };

    render() {
        const form = this.props.form;

        return (
            <form>
                {form.params.map((param, i) => {
                    return <Param
                        key={i}
                        param={param}
                        onChange={this.changeParam(i)}
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