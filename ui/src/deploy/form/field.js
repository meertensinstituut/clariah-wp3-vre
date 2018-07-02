import React from "react";
import String from "./string";
import Select from "./select";
import Integer from "./integer";

export default class Field extends React.Component {

    constructor(props) {
        super(props);
        this.state = {}
    }

    render() {
        let field = null;
        if (this.props.param.type === "string") {
            field = <String param={this.props.param}/>;
        } else if (this.props.param.type === "integer") {
            field = <Integer param={this.props.param} />;
        } else if (this.props.param.type === "enumeration") {
            field = <Select param={this.props.param} />
        }
        return (
            <div>
                <pre>{JSON.stringify(this.props.param)}</pre>
                {field}
            </div>
        );
    }
}