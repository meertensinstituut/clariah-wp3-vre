import React from "react";
import {ControlLabel, FormControl, FormGroup, HelpBlock} from 'react-bootstrap';

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
        console.log('change!');
        this.props.onChange(e.target.value);
        this.setState({value: e.target.value});
    }

    render() {
        let multiple = this.props.param.maximumCardinality <= 1;
        return (
            <FormGroup controlId="formControlsSelect">
                <ControlLabel>{this.props.param.label}</ControlLabel>
                <FormControl.Feedback/>
                <HelpBlock>{this.props.param.description}</HelpBlock>
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