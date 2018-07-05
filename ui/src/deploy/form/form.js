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

    addParam = (indexToCopy) => () => {
        let form = this.props.form;
        let copy = JSON.parse(JSON.stringify(form.params[indexToCopy]));
        copy.value = [""];
        this.setAddableAndRemoveable(copy, form.params);
        form.params.forEach((p) => {
            if(p.name === copy.name) {
                this.setAddableAndRemoveable(p, form.params);
            }
        });
        copy.params.forEach((p) => {
            p.value = [""];
            this.setAddableAndRemoveable(p, form.params);
        });
        form.params.splice(indexToCopy + 1, 0, copy);
        this.props.onChange(form);
    };

    removeParam = (indexToRemove) => () => {
        let form = this.props.form;
        form.params.splice(indexToRemove, 1);
        this.props.onChange(form);
    };

    setAddableAndRemoveable(param, siblings) {
        param.canAdd = this.canAddParam(param, siblings);
        param.canRemove = this.canRemoveParam(param, siblings);
    }

    canRemoveParam(param, siblings) {
        let min = Number(param.minimumCardinality);
        if(min === 0) {
            return true;
        }
        let hasChildParams = Array.isArray(param.params);
        if(!hasChildParams && param.value.length > min) {
            return true;
        }
        let copies = siblings.filter((p) => p.name === param.name).length;
        if(hasChildParams && min < copies) {
            return true;
        }
        throw new Error('Could not determine if param can be removed');
    }

    canAddParam(param, siblings) {
        let max = Number(param.maximumCardinality);
        if(param.maximumCardinality === '*') {
            return true;
        }
        let hasChildParams = Array.isArray(param.params);
        if(!hasChildParams && param.value.length < max) {
            return true;
        }
        let copies = siblings.filter((p) => p.name === param.name).length;
        if(hasChildParams && max > copies) {
            return true;
        }
        throw new Error('Could not determine if param can be added');
    }

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