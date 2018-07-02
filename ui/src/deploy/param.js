import React from "react";
import Field from "./form/field";

export default class Param extends React.Component {

    constructor(props) {
        super(props);
        this.state = {};
        this.onChange = this.onChange.bind(this);
        this.onChangeChild = this.onChangeChild.bind(this);
    }

    onChange(param) {
        this.props.onChange(param);
    }

    onChangeChild(child) {
        let param = this.props.param;
        let childIndex = param.params.findIndex(
            (p) => Number(p.id) === Number(child.id)
        );
        param.params[childIndex] = child;
        this.props.onChange(param);
    }

    render() {
        let param = this.props.param;
        let children = param.params.length > 0
            ?
            param.params.map((childParam, k) => {
                return <Field
                    key={k}
                    param={childParam}
                    onChange={this.onChange}
                />
            }, this)
            :
            null;

        return (
            <div>
                <Field
                    param={param}
                    onChange={this.onChangeChild}
                />
                {children}
            </div>
        );
    }
}