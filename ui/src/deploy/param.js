import React from "react";
import Field from "./form/field";
import PropTypes from 'prop-types';
import LeafParam from "./form/leaf-param";

/**
 * Param with child params
 */
export default class Param extends React.Component {

    change = (param) => {
        this.props.onChange(param);
    };

    changeChild = (index) => (child) => {
        let param = this.props.param;
        param.params[index] = child;
        this.change(param);
    };

    add = () => {
        this.props.onAdd();
    };

    remove = () => {
        this.props.onRemove();
    };

    renderMultipleValues(param) {
        return <LeafParam
            param={param}
            onChange={this.change}
        />;
    }

    renderParentField(param) {
        return <Field
            index={0}
            param={param}
            onChange={this.change}
            onAdd={this.add}
            onRemove={this.remove}
            bare={false}
        />;
    }

    renderChildParams(param) {
        return param.params.map((childParam, i) => {
            return <LeafParam
                key={i}
                param={childParam}
                onChange={this.changeChild()}
            />;
        }, this);
    }

    render() {
        let param = this.props.param;
        let hasChildren = Array.isArray(param.params);
        if (hasChildren) {
            return (
                <div>
                    {this.renderParentField(param)}
                    {this.renderChildParams(param)}
                </div>
            );
        } else {
            return (
                <LeafParam
                    param={param}
                    onChange={this.change}
                />
            );
        }
    }
}

Field.propTypes = {
    param: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
    onAdd: PropTypes.func.isRequired
};