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
        this.props.param.value.push("");
        this.change(param);
    };

    addChild = (index) => () => {
        let param = this.props.param;
        param.params[index].value.push("");
        this.change(param);
    };

    render() {
        let param = this.props.param;
        let children = Array.isArray(param.params)
            ?
            param.params.map((param, i) => {
                return param.value.map((childParam, k) => {
                    return <Field
                        key={k}
                        index={k}
                        param={param}
                        onChange={this.changeChild(i)}
                        onAdd={this.addChild(i)}
                        bare={k > 0}
                    />
                }, this)
            }, this)
            :
            null;

        return (
            <div>
                {param.value.map((value, i) => {
                    return <Field
                        key={i}
                        index={i}
                        param={param}
                        onChange={this.change}
                        onAdd={this.add}
                        bare={i > 0}
                    />
                }, this)}
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