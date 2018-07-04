import React from "react";
import {FormControl, FormGroup} from 'react-bootstrap';

export default class Select extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            value: ""
        };
        this.handleChange = this.handleChange.bind(this);
    }

    handleValidationFormBasicText() {
        return null;
    }

    handleChange(e) {
        this.props.onChange(e.target.value);
        this.setState({value: e.target.value});
    }

    render() {
        let multiple = this.props.param.maximumCardinality <= 1;
        return (
            <FormGroup controlId="formControlsSelect">
                <FormControl.Feedback/>
                <FormControl
                    componentClass="select"
                    placeholder="select"
                    multiple={multiple}
                    onChange={this.handleChange}
                >
                    {this.props.param.values.map(function (value, i) {
                        return (
                            <option key={i} value={value.value}>
                                {value.label}
                                {(value.label!==undefined && value.description !== undefined) ? " - " : ""}
                                {value.description}
                            </option>
                        );
                    }, this)}
                </FormControl>
            </FormGroup>
        );
    }
}