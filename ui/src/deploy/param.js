import React from "react";
import Field from "./form/field";
import PropTypes from 'prop-types';

export default class Param extends React.Component {

    constructor(props) {
        super(props);
        this.state = {};
        this.change = this.change.bind(this);
        this.changeChild = this.changeChild.bind(this);
        this.add = this.add.bind(this);
    }

    change(param) {
        this.props.onChange(param);
    }

    changeChild(child) {
        let param = this.props.param;
        let childIndex = param.params.findIndex(
            (p) => Number(p.id) === Number(child.id)
        );
        param.params[childIndex] = child;
        this.props.onChange(param);
    }

    add(param) {
        this.props.onAdd(param);
    }

    render() {
        let param = this.props.param;
        let children = Array.isArray(param.params)
            ?
            param.params.map((childParam, k) => {
                return <Field
                    key={k}
                    param={childParam}
                    onChange={this.change}
                    canAdd={true}
                    onAdd={this.add}
                />
            }, this)
            :
            null;

        return (
            <div>
                <Field
                    param={param}
                    onChange={this.change}
                    canAdd={true}
                    onAdd={this.add}
                />
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