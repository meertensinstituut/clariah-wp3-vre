import React from "react";
import String from "./string";
import Select from "./select";
import Integer from "./integer";
import PropTypes from 'prop-types';

export default class Field extends React.Component {

    constructor(props) {
        super(props);
        this.state = {};
        this.onChange = this.onChange.bind(this);
    }

    onChange(e) {
        this.props.param.value = e;
        this.props.onChange(this.props.param);
    }

    render() {
        let field = null;
        if (this.props.param.type === "string") {
            field = <String param={this.props.param} onChange={this.onChange} />;
        } else if (this.props.param.type === "integer") {
            field = <Integer param={this.props.param} onChange={this.onChange} />;
        } else if (this.props.param.type === "enumeration") {
            field = <Select param={this.props.param} onChange={this.onChange} />
        }
        return (
            <div>
                <pre>{JSON.stringify(this.props.param)}</pre>
                {field}
            </div>
        );
    }
}

Field.propTypes = {
    param: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired
};